package no.fintlabs.extensions

import com.fasterxml.jackson.databind.ObjectMapper
import io.fabric8.crd.generator.CRDGenerator
import io.fabric8.crd.generator.CRDGenerator.AbstractCRDOutput
import io.fabric8.kubeapitest.KubeAPIServer
import io.fabric8.kubeapitest.KubeAPIServerConfigBuilder
import io.fabric8.kubeapitest.KubeAPIServerConfigBuilder.KUBE_API_TEST_OFFLINE_MODE
import io.fabric8.kubernetes.api.model.Namespace
import io.fabric8.kubernetes.client.Config
import io.fabric8.kubernetes.client.CustomResource
import io.fabric8.kubernetes.client.KubernetesClient
import io.fabric8.kubernetes.client.KubernetesClientBuilder
import io.fabric8.kubernetes.client.utils.KubernetesSerialization
import io.javaoperatorsdk.operator.Operator
import io.javaoperatorsdk.operator.api.reconciler.Reconciler
import io.javaoperatorsdk.operator.junit.DefaultNamespaceNameSupplier
import org.awaitility.kotlin.atMost
import org.awaitility.kotlin.await
import org.awaitility.kotlin.until
import org.junit.jupiter.api.extension.*
import org.koin.core.component.KoinComponent
import org.koin.core.component.get
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.time.Duration

class KubernetesOperatorExtension
private constructor(private val crdClass: List<Class<out CustomResource<*, *>>>) : BeforeEachCallback,
    BeforeAllCallback, AfterAllCallback, AfterEachCallback, ParameterResolver, KoinComponent {
    private val kubernetesApi = KubeAPIServer(KubeAPIServerConfigBuilder().apply {
        withOfflineMode(System.getenv(KUBE_API_TEST_OFFLINE_MODE).toBoolean())
    }.build())
    private val namespaceSupplier = DefaultNamespaceNameSupplier()

    private var additionalResources = emptyList<KubernetesResourceSource>()

    override fun beforeAll(context: ExtensionContext) {
        prepareAdditionalResources(context)
        kubernetesApi.start()
        ensureCRDs()
    }

    override fun afterAll(context: ExtensionContext) {
        kubernetesApi.stop()
    }

    override fun beforeEach(context: ExtensionContext) {
        val namespace = namespaceSupplier.apply(context)
        val kubernetesClient = createKubernetesClient(namespace, get())

        prepareKoin(kubernetesClient)
        prepareKubernetes(kubernetesClient, namespace)
        applyAdditionalResources(kubernetesClient, namespace)
        context.store()
            .put(KubernetesOperatorContext::class.simpleName, KubernetesOperatorContext(namespace, kubernetesClient))

        get<Operator>().start()
    }

    override fun afterEach(context: ExtensionContext) {
        val kubernetesClient = get<KubernetesClient>()
        val kubernetesOperatorContext =
            context.store().get(KubernetesOperatorContext::class.simpleName) as KubernetesOperatorContext

        cleanupKubernetes(kubernetesClient, kubernetesOperatorContext.namespace)

        get<Operator>().stop()
    }

    override fun supportsParameter(pContext: ParameterContext, eContext: ExtensionContext): Boolean =
        pContext.parameter.type == KubernetesOperatorContext::class.java

    override fun resolveParameter(pContext: ParameterContext, eContext: ExtensionContext): Any =
        eContext.store().get(KubernetesOperatorContext::class.simpleName)


    private fun ensureCRDs() {
        val crds = prepareCRDs()
        val kubernetesClient = createKubernetesClient()
        crds.forEach { crd ->
            kubernetesClient.load(ByteArrayInputStream(crd.toByteArray())).serverSideApply()
        }
        await atMost Duration.ofSeconds(30) until { crds.all { kubernetesClient.resource(it).get() != null } }

        kubernetesClient.close()
    }

    private fun prepareCRDs(): List<String> {
        val output = InMemoryCRDOutput()
        val crdGenerator = CRDGenerator()
            .customResourceClasses(*crdClass.toTypedArray())
            .withOutput(output)
            .forCRDVersions("v1")
        crdGenerator.generate()
        return output.getCRDs().also { output.close() }
    }

    private fun prepareKoin(kubernetesClient: KubernetesClient) {
        getKoin().apply {
            declare(kubernetesClient)
            declare<(Operator) -> Unit>(
                { operator ->
                    getAll<Reconciler<*>>().forEach {
                        operator.register(it) { config ->
                            config.settingNamespace(kubernetesClient.namespace)
                        }
                    }
                })
        }
    }

    private fun prepareKubernetes(kubernetesClient: KubernetesClient, namespace: String) {
        val namespaceResource = Namespace().apply {
            metadata = io.fabric8.kubernetes.api.model.ObjectMeta().apply {
                name = namespace
            }
        }
        kubernetesClient.namespaces().resource(namespaceResource).create()
    }

    private fun cleanupKubernetes(kubernetesClient: KubernetesClient, namespace: String) {
        kubernetesClient.namespaces().withName(namespace).delete()
    }

    private fun ExtensionContext.store(): ExtensionContext.Store {
        return this.getStore(KUBERNETES_OPERATOR_STORE)
    }

    private fun createKubernetesClient(namespace: String? = null, serializer: ObjectMapper? = null): KubernetesClient {
        return KubernetesClientBuilder().apply {
            withConfig(Config.fromKubeconfig(kubernetesApi.kubeConfigYaml).apply {
                setNamespace(namespace)
            })
            if (serializer != null) {
                withKubernetesSerialization(KubernetesSerialization(serializer, true))
            }
        }.build()
    }

    private fun prepareAdditionalResources(context: ExtensionContext) {
        context.element.ifPresent {
            it.getAnnotation(KubernetesResources::class.java)?.let { kubernetesResource ->
                additionalResources = KubernetesResourceSource.fromResources(kubernetesResource.paths.toList())
            }
        }
    }

    private fun applyAdditionalResources(kubernetesClient: KubernetesClient, namespace: String) {
        additionalResources.forEach { resource ->
            resource.open()?.use { inputStream ->
                kubernetesClient.load(inputStream).inNamespace(namespace).serverSideApply()
            }
        }
    }

    companion object {
        fun create(crdClass: List<Class<out CustomResource<*, *>>> = emptyList()) =
            KubernetesOperatorExtension(crdClass)

        private val KUBERNETES_OPERATOR_STORE: ExtensionContext.Namespace =
            ExtensionContext.Namespace.create("KUBERNETES_OPERATOR_STORE")
    }
}

class InMemoryCRDOutput : AbstractCRDOutput<ByteArrayOutputStream>() {
    private val streams = mutableListOf<ByteArrayOutputStream>()

    override fun createStreamFor(crdName: String): ByteArrayOutputStream {
        return ByteArrayOutputStream().also {
            streams.add(it)
        }
    }

    fun getCRDs(): List<String> {
        return streams.map { it.toString() }
    }
}
