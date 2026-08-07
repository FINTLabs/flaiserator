package no.fintlabs.application

import io.traefik.v1alpha1.IngressRoute
import no.fintlabs.Config
import no.fintlabs.IngressConfig
import no.fintlabs.application.Utils.createAndGetResource
import no.fintlabs.application.Utils.createApplicationKoinTestExtension
import no.fintlabs.application.Utils.createApplicationKubernetesOperatorExtension
import no.fintlabs.application.Utils.createTestFlaisApplication
import no.fintlabs.application.api.v1alpha1.FlaisApplication
import no.fintlabs.application.api.v1alpha1.Ingress
import no.fintlabs.extensions.KubernetesOperatorContext
import org.junit.jupiter.api.extension.RegisterExtension
import org.koin.dsl.module
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class IngressV3DRTest {
    @Test
    fun `should create simple IngressRoute`(context: KubernetesOperatorContext) {
        val flaisApplication =
            createTestFlaisApplication().apply {
                spec =
                    spec.copy(
                        ingress = Ingress(routes = listOf(Ingress.Route("test.example.com", "/test"))),
                    )
            }

        val ingressRoute = context.createAndGetIngressRoute(flaisApplication)
        assertNotNull(ingressRoute)
        assertEquals("test", ingressRoute.metadata.name)
        assertEquals("web", ingressRoute.spec.entryPoints[0])
        assertEquals(
            "Host(`test.example.com`) && PathPrefix(`/test`)",
            ingressRoute.spec.routes[0].match,
        )
        assertEquals(
            8080,
            ingressRoute.spec.routes[0]
                .services[0]
                .port.intVal,
        )
        assertEquals(
            "test",
            ingressRoute.spec.routes[0]
                .services[0]
                .name,
        )
        assertEquals(
            context.namespace,
            ingressRoute.spec.routes[0]
                .services[0]
                .namespace,
        )
    }

    @Test
    fun `should create IngressRoute with regex header`(context: KubernetesOperatorContext) {
        val flaisApplication =
            createTestFlaisApplication().apply {
                spec =
                    spec.copy(
                        ingress =
                            Ingress(
                                routes =
                                    listOf(
                                        Ingress.Route(
                                            host = "test.example.com",
                                            path = "/test",
                                            headers = mapOf("header" to "re:value.*"),
                                        ),
                                    ),
                            ),
                    )
            }

        val ingressRoute = context.createAndGetIngressRoute(flaisApplication)
        assertNotNull(ingressRoute)
        assertEquals(
            "Host(`test.example.com`) && PathPrefix(`/test`) && HeadersRegexp(`header`, `value.*`)",
            ingressRoute.spec.routes[0].match,
        )
    }

    @Test
    fun `should not create IngressRoute`(context: KubernetesOperatorContext) {
        val flaisApplication = createTestFlaisApplication()

        val ingressRoute = context.createAndGetIngressRoute(flaisApplication)
        assertNull(ingressRoute)
    }

    private fun KubernetesOperatorContext.createAndGetIngressRoute(app: FlaisApplication) = createAndGetResource<IngressRoute>(app)

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
