package no.fintlabs.operator

import io.fabric8.kubeapitest.KubeAPIServer
import io.fabric8.kubernetes.api.model.Namespace
import io.fabric8.kubernetes.client.KubernetesClient
import io.fabric8.kubernetes.client.KubernetesClientBuilder
import io.javaoperatorsdk.operator.Operator
import io.javaoperatorsdk.operator.junit.DefaultNamespaceNameSupplier
import org.junit.jupiter.api.extension.*
import org.koin.core.component.KoinComponent
import org.koin.core.component.get

class KubernetesOperatorExtension private constructor() : BeforeEachCallback, BeforeAllCallback, AfterAllCallback, AfterEachCallback, ParameterResolver, KoinComponent {
    private val kubernetesApi = KubeAPIServer()
    private val namespaceSupplier = DefaultNamespaceNameSupplier()

    override fun beforeAll(context: ExtensionContext) {
        kubernetesApi.start()
    }

    override fun afterAll(context: ExtensionContext) {
        kubernetesApi.stop()
    }

    override fun beforeEach(context: ExtensionContext) {
        val kubernetesClient = KubernetesClientBuilder().withConfig(kubernetesApi.kubeConfigYaml).build()
        val namespace = namespaceSupplier.apply(context)

        prepareKoin(kubernetesClient)
        prepareKubernetes(kubernetesClient, namespace)
        context.store().put(KubernetesOperatorContext::class.simpleName, KubernetesOperatorContext(namespace, kubernetesClient))

        get<Operator>().start()
    }

    override fun afterEach(context: ExtensionContext) {
        val kubernetesClient = get<KubernetesClient>()
        val kubernetesOperatorContext = context.store().get(KubernetesOperatorContext::class.simpleName) as KubernetesOperatorContext

        cleanupKubernetes(kubernetesClient, kubernetesOperatorContext.namespace)

        get<Operator>().stop()
    }

    private fun prepareKoin(kubernetesClient: KubernetesClient) {
        getKoin().declare(kubernetesClient)
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

    override fun supportsParameter(pContext: ParameterContext, eContext: ExtensionContext): Boolean =
        pContext.parameter.type == KubernetesOperatorContext::class.java

    override fun resolveParameter(pContext: ParameterContext, eContext: ExtensionContext): Any =
        eContext.store().get(KubernetesOperatorContext::class.simpleName)


    private fun ExtensionContext.store(): ExtensionContext.Store {
        return this.getStore(KUBERNETES_OPERATOR_STORE)
    }

    companion object {
        fun create() = KubernetesOperatorExtension()

        private val KUBERNETES_OPERATOR_STORE: ExtensionContext.Namespace = ExtensionContext.Namespace.create("KUBERNETES_OPERATOR_STORE")
    }
}
