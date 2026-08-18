package no.fintlabs.application.ingress

import no.fintlabs.Config
import no.fintlabs.IngressConfig
import no.fintlabs.application.Utils.createApplicationKoinTestExtension
import no.fintlabs.application.Utils.createApplicationKubernetesOperatorExtension
import no.fintlabs.application.api.v1alpha1.FlaisApplication
import no.fintlabs.extensions.KubernetesOperatorContext
import org.junit.jupiter.api.extension.RegisterExtension
import org.koin.dsl.module

class IngressV3LegacyDRTest : BaseLegacyIngressRouteDRTest() {
    override fun KubernetesOperatorContext.createAndGetIngressRouteView(app: FlaisApplication) =
        createAndGetV3IngressRoute(app)?.toIngressRouteView()

    companion object {
        @RegisterExtension
        val koinTestExtension =
            createApplicationKoinTestExtension(
                module {
                    single {
                        Config(ingress = IngressConfig(traefikV3CrdSupport = true))
                    }
                },
            )

        @RegisterExtension
        val kubernetesOperatorExtension = createApplicationKubernetesOperatorExtension()
    }
}
