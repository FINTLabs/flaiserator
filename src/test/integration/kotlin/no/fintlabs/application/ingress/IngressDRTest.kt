package no.fintlabs.application.ingress

import no.fintlabs.application.Utils.createApplicationKoinTestExtension
import no.fintlabs.application.Utils.createApplicationKubernetesOperatorExtension
import no.fintlabs.application.api.v1alpha1.FlaisApplication
import no.fintlabs.extensions.KubernetesOperatorContext
import org.junit.jupiter.api.extension.RegisterExtension

class IngressDRTest : BaseIngressRouteDRTest() {
    override fun KubernetesOperatorContext.createAndGetIngressRouteView(app: FlaisApplication) =
        createAndGetLegacyIngressRoute(app)?.toIngressRouteView()

    companion object {
        @RegisterExtension val koinTestExtension = createApplicationKoinTestExtension()

        @RegisterExtension
        val kubernetesOperatorExtension = createApplicationKubernetesOperatorExtension()
    }
}
