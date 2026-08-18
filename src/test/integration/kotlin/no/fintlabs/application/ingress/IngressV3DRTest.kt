package no.fintlabs.application.ingress

import io.javaoperatorsdk.operator.health.Status
import io.javaoperatorsdk.operator.processing.event.source.EventSource
import io.traefik.v1alpha1.IngressRoute
import no.fintlabs.Config
import no.fintlabs.IngressConfig
import no.fintlabs.application.Utils.createApplicationKoinTestExtension
import no.fintlabs.application.Utils.createApplicationKubernetesOperatorExtension
import no.fintlabs.application.api.MANAGED_BY_FLAISERATOR_LABEL
import no.fintlabs.application.api.v1alpha1.FlaisApplication
import no.fintlabs.extensions.KubernetesOperatorContext
import org.junit.jupiter.api.extension.RegisterExtension
import org.koin.dsl.module
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import us.containo.traefik.v1alpha1.IngressRoute as LegacyIngressRoute

class IngressV3DRTest : BaseIngressRouteDRTest() {
    @Test
    fun `should register distinct Traefik v2 and v3 informer event sources`(context: KubernetesOperatorContext) {
        val informerSources =
            context.operator
                .getRegisteredController("applicationreconciler")
                .orElseThrow()
                .getControllerHealthInfo()
                .informerEventSourceHealthIndicators()

        assertContains(informerSources.keys, "ingress-v2")
        assertContains(informerSources.keys, "ingress-v3")

        val ingressV2Source = assertNotNull(informerSources["ingress-v2"])
        val ingressV3Source = assertNotNull(informerSources["ingress-v3"])

        assertEquals(Status.HEALTHY, ingressV2Source.status)
        assertEquals(Status.HEALTHY, ingressV3Source.status)
        assertEquals(LegacyIngressRoute::class.java, (ingressV2Source as EventSource<*, *>).resourceType())
        assertEquals(IngressRoute::class.java, (ingressV3Source as EventSource<*, *>).resourceType())
    }

    @Test
    fun `should create Traefik v2 and v3 IngressRoutes when v3 support is enabled`(context: KubernetesOperatorContext) {
        val flaisApplication = simpleIngressApplication()

        val ingressRouteV3 = context.createAndGetV3IngressRoute(flaisApplication)
        val ingressRouteV2 = context.get<LegacyIngressRoute>(flaisApplication.metadata.name)

        assertNotNull(ingressRouteV2)
        assertNotNull(ingressRouteV3)
        assertEquals(
            "Host(`test.example.com`) && PathPrefix(`/test`)",
            ingressRouteV2.spec.routes[0].match,
        )
        assertEquals(
            "Host(`test.example.com`) && PathPrefix(`/test`)",
            ingressRouteV3.spec.routes[0].match,
        )
    }

    @Test
    fun `should reconcile existing IngressRoute outside informer cache`(context: KubernetesOperatorContext) {
        val flaisApplication = simpleIngressApplication()

        context.create(context.unmanagedV3IngressRoute(flaisApplication))

        val ingressRoute = context.createAndGetV3IngressRoute(flaisApplication)
        assertNotNull(ingressRoute)
        assertEquals(MANAGED_BY_FLAISERATOR_LABEL.second, ingressRoute.metadata.labels[MANAGED_BY_FLAISERATOR_LABEL.first])
        assertEquals(
            "Host(`test.example.com`) && PathPrefix(`/test`)",
            ingressRoute.spec.routes[0].match,
        )
    }

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
