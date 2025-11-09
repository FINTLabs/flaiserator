package no.fintlabs.extensions

import com.fasterxml.jackson.databind.ObjectMapper
import io.fabric8.crdv2.generator.CRDGenerator
import io.fabric8.kubernetes.api.model.Namespace
import io.fabric8.kubernetes.client.Config
import io.fabric8.kubernetes.client.CustomResource
import io.fabric8.kubernetes.client.KubernetesClient
import io.fabric8.kubernetes.client.KubernetesClientBuilder
import io.fabric8.kubernetes.client.utils.KubernetesSerialization
import io.javaoperatorsdk.operator.Operator
import io.javaoperatorsdk.operator.api.reconciler.Reconciler
import io.javaoperatorsdk.operator.junit.DefaultNamespaceNameSupplier
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.time.Duration
import kotlin.jvm.optionals.getOrNull
import no.fintlabs.common.getLogger
import no.fintlabs.extensions.Utils.executeWithRetry
import no.fintlabs.operator.OperatorConfigHandler
import no.fintlabs.operator.OperatorPostConfigHandler
import org.awaitility.kotlin.atMost
import org.awaitility.kotlin.await
import org.awaitility.kotlin.until
import org.junit.jupiter.api.extension.*
import org.koin.core.component.KoinComponent
import org.koin.core.component.get
import org.koin.core.component.getScopeName
import org.koin.core.context.loadKoinModules
import org.koin.core.qualifier.named
import org.koin.dsl.module
import org.testcontainers.k3s.K3sContainer
import org.testcontainers.utility.DockerImageName

class KubernetesOperatorExtension
private constructor(private val crdClass: List<Class<out CustomResource<*, *>>>) :
    BeforeEachCallback,
    BeforeAllCallback,
    AfterAllCallback,
    AfterEachCallback,
    ParameterResolver,
    KoinComponent {
  private val logger = getLogger()
  private val k3s: K3sContainer
  private val namespaceSupplier = DefaultNamespaceNameSupplier()

  private var classAdditionalResources = emptyList<KubernetesResourceSource>()

  init {
    val kubernetesVersion = System.getenv("TEST_KUBERNETES_VERSION")?.let { "$it-k3s1" } ?: "latest"
    k3s = K3sContainer(DockerImageName.parse("rancher/k3s:$kubernetesVersion")).withReuse(true)
  }

  override fun beforeAll(context: ExtensionContext) {
    classAdditionalResources = getAdditionalResources(context)
    if (!useLocalKubernetes()) {
      k3s.start()
    }
    ensureCRDs()
  }

  override fun afterAll(context: ExtensionContext) {
    if (k3s.isRunning) {
      k3s.stop()
    }
  }

  override fun beforeEach(context: ExtensionContext) {
    val operatorConfig = getKubernetesOperatorConfig(context)
    val namespace = namespaceSupplier.apply(context)
    val kubernetesClient = createKubernetesClient(namespace, get())

    prepareKoin(kubernetesClient, operatorConfig.registerReconcilers)
    prepareKubernetes(kubernetesClient, namespace)
    val testAdditionalResources = getAdditionalResources(context)
    applyAdditionalResources(kubernetesClient, namespace, testAdditionalResources)

    val operatorContext =
        KubernetesOperatorContext(namespace, { get<KubernetesClient>() }, { get<Operator>() })
    context.store().put(KubernetesOperatorContext::class.simpleName, operatorContext)

    if (!operatorConfig.explicitStart) {
      startOperator(operatorContext.operator)
    }
  }

  override fun afterEach(context: ExtensionContext) {
    val kubernetesOperatorContext =
        context.store().get(KubernetesOperatorContext::class.simpleName)
            as KubernetesOperatorContext
    val kubernetesClient = kubernetesOperatorContext.kubernetesClient

    try {
      cleanupKubernetes(kubernetesClient, kubernetesOperatorContext.namespace)
      kubernetesOperatorContext.operator.stop()
    } finally {
      kubernetesClient.close()
    }
  }

  override fun supportsParameter(pContext: ParameterContext, eContext: ExtensionContext): Boolean =
      pContext.parameter.type == KubernetesOperatorContext::class.java

  override fun resolveParameter(pContext: ParameterContext, eContext: ExtensionContext): Any =
      eContext.store().get(KubernetesOperatorContext::class.simpleName)

  private fun startOperator(operator: Operator) {
    executeWithRetry { operator.start() }
  }

  private fun ensureCRDs() {
    val crds = prepareCRDs()
    createKubernetesClient().use { kubernetesClient ->
      crds.forEach { crd ->
        kubernetesClient.load(ByteArrayInputStream(crd.toByteArray())).serverSideApply()
      }
      await atMost
          Duration.ofSeconds(30) until
          {
            crds.all { kubernetesClient.resource(it).get() != null }
          }
    }
  }

  private fun prepareCRDs(): List<String> =
      InMemoryCRDOutput().use { output ->
        val crdGenerator =
            CRDGenerator()
                .customResourceClasses(*crdClass.toTypedArray())
                .withOutput(output)
                .forCRDVersions("v1")
        crdGenerator.generate()
        return output.getCRDs()
      }

  private fun prepareKoin(kubernetesClient: KubernetesClient, registerReconcilers: Boolean) {
    getKoin().apply {
      loadKoinModules(
          module {
            single { kubernetesClient }
            single(named("test")) {
              OperatorConfigHandler {
                it.setCloseClientOnStop(false)
                it.setReconciliationTerminationTimeout(Duration.ofMillis(100))
              }
            }
            single {
              OperatorPostConfigHandler { operator ->
                if (registerReconcilers) {
                  getAll<Reconciler<*>>().forEach {
                    operator.register(it) { config ->
                      config.settingNamespace(kubernetesClient.namespace)
                    }
                  }
                }
              }
            }
          }
      )
    }
  }

  private fun prepareKubernetes(kubernetesClient: KubernetesClient, namespace: String) {
    val namespaceResource =
        Namespace().apply {
          metadata = io.fabric8.kubernetes.api.model.ObjectMeta().apply { name = namespace }
        }
    kubernetesClient.namespaces().resource(namespaceResource).create()
  }

  private fun cleanupKubernetes(kubernetesClient: KubernetesClient, namespace: String) {
    kubernetesClient.namespaces().withName(namespace).delete()
  }

  private fun ExtensionContext.store(): ExtensionContext.Store =
      this.getStore(KUBERNETES_OPERATOR_STORE)

  private fun createKubernetesClient(
      namespace: String? = null,
      serializer: ObjectMapper? = null,
  ): KubernetesClient =
      KubernetesClientBuilder()
          .apply {
            withConfig(getKubernetesConfig().apply { setNamespace(namespace) })
            if (serializer != null) {
              withKubernetesSerialization(KubernetesSerialization(serializer, true))
            }
          }
          .build()

  private fun getKubernetesConfig() =
      if (useLocalKubernetes()) {
        logger.debug("Using local kubernetes config")
        Config.autoConfigure(null)
      } else {
        Config.fromKubeconfig(k3s.kubeConfigYaml)
      }

  private fun getAdditionalResources(context: ExtensionContext) =
      context.element.getOrNull()?.getAnnotation(KubernetesResources::class.java)?.paths?.let {
        KubernetesResourceSource.fromResources(it.toList())
      } ?: emptyList()

  private fun applyAdditionalResources(
      kubernetesClient: KubernetesClient,
      namespace: String,
      testAdditionalResources: List<KubernetesResourceSource>,
  ) {
    val additionalResources = testAdditionalResources + classAdditionalResources
    additionalResources.forEach { resource ->
      resource.open()?.use { inputStream ->
        kubernetesClient.load(inputStream).inNamespace(namespace).serverSideApply()
      }
    }
  }

  private fun getKubernetesOperatorConfig(context: ExtensionContext): KubernetesOperator =
      context.requiredTestMethod.getAnnotation(KubernetesOperator::class.java)
          ?: KubernetesOperator()

  private fun useLocalKubernetes() = System.getenv("TEST_KUBERNETES_LOCAL").toBoolean()

  companion object {
    fun create(crdClass: List<Class<out CustomResource<*, *>>> = emptyList()) =
        KubernetesOperatorExtension(crdClass)

    private val KUBERNETES_OPERATOR_STORE: ExtensionContext.Namespace =
        ExtensionContext.Namespace.create("KUBERNETES_OPERATOR_STORE")
  }
}

class InMemoryCRDOutput : CRDGenerator.AbstractCRDOutput<ByteArrayOutputStream>() {
  private val streams = mutableListOf<ByteArrayOutputStream>()

  override fun createStreamFor(crdName: String): ByteArrayOutputStream =
      ByteArrayOutputStream().also { streams.add(it) }

  fun getCRDs(): List<String> = streams.map { it.toString() }
}
