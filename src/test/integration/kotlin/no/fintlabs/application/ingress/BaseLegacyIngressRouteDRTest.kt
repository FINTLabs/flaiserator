package no.fintlabs.application.ingress

import io.fabric8.kubernetes.client.KubernetesClientException
import no.fintlabs.application.Utils.createTestFlaisApplication
import no.fintlabs.application.api.v1alpha1.FlaisApplication
import no.fintlabs.application.api.v1alpha1.Ingress
import no.fintlabs.application.api.v1alpha1.Url
import no.fintlabs.extensions.KubernetesOperatorContext
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Tags
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

abstract class BaseLegacyIngressRouteDRTest {
    @Test
    @Tags(Tag("legacy-ingress"))
    fun `should create IngressRoute`(context: KubernetesOperatorContext) {
        val ingressRoute = context.createAndGetIngressRouteView(legacyIngressApplication())

        assertNotNull(ingressRoute)
        assertSimpleIngressRoute(ingressRoute, context.namespace)
    }

    @Test
    fun `should not create IngressRoute since enabled is false`(context: KubernetesOperatorContext) {
        val ingressRoute = context.createAndGetIngressRouteView(legacyIngressApplication(enabled = false))

        assertNull(ingressRoute)
    }

    @Test
    fun `should not create IngressRoute since url is not set`(context: KubernetesOperatorContext) {
        val ingressRoute = context.createAndGetIngressRouteView(legacyIngressApplication(host = null))

        assertNull(ingressRoute)
    }

    @Test
    fun `should not accept invalid hostname`(context: KubernetesOperatorContext) {
        val flaisApplication = legacyIngressApplication(host = "notAValidHostname/&%")

        try {
            context.create(flaisApplication)
        } catch (e: KubernetesClientException) {
            assertEquals(422, e.code)
            assertEquals("Invalid", e.status.reason)
            assert(e.status.message.contains("Invalid hostname"))
        }
    }

    @Test
    fun `should not accept invalid path`(context: KubernetesOperatorContext) {
        val flaisApplication =
            createTestFlaisApplication().apply {
                spec =
                    spec.copy(
                        url = Url("test.example.com", "notAValidPath/&%=##dfnjkdkjn44"),
                        ingress = Ingress(true),
                    )
            }

        try {
            context.create(flaisApplication)
        } catch (e: KubernetesClientException) {
            assertEquals(422, e.code)
            assertEquals("Invalid", e.status.reason)
            assert(e.status.message.contains("Invalid path"))
        }
    }

    @Test
    fun `should create IngressRoute with default path`(context: KubernetesOperatorContext) {
        val ingressRoute = context.createAndGetIngressRouteView(legacyIngressApplication(path = null))

        assertNotNull(ingressRoute)
        assertEquals("Host(`test.example.com`)", ingressRoute.routes[0].match)
    }

    @Test
    fun `should create IngressRoute with custom path`(context: KubernetesOperatorContext) {
        val ingressRoute = context.createAndGetIngressRouteView(legacyIngressApplication())

        assertNotNull(ingressRoute)
        assertEquals(
            "Host(`test.example.com`) && PathPrefix(`/test`)",
            ingressRoute.routes[0].match,
        )
    }

    protected abstract fun KubernetesOperatorContext.createAndGetIngressRouteView(app: FlaisApplication): IngressRouteView?
}
