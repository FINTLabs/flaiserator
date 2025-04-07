package no.fintlabs.application

import io.fabric8.kubernetes.client.KubernetesClientException
import no.fintlabs.extensions.KubernetesOperatorContext
import no.fintlabs.application.Utils.createAndGetResource
import no.fintlabs.application.Utils.createKoinTestExtension
import no.fintlabs.application.Utils.createKubernetesOperatorExtension
import no.fintlabs.application.Utils.createTestFlaisApplication
import no.fintlabs.application.api.v1alpha1.FlaisApplicationCrd
import no.fintlabs.application.api.v1alpha1.Ingress
import no.fintlabs.application.api.v1alpha1.Url
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Tags
import org.junit.jupiter.api.extension.RegisterExtension
import us.containo.traefik.v1alpha1.IngressRoute
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class LegacyIngressDRTest{
    //region General
    @Test
    @Tags(Tag("legacy-ingress"))
    fun `should create IngressRoute`(context: KubernetesOperatorContext) {
        val flaisApplication = createTestFlaisApplication().apply {
            spec = spec.copy(url = Url("test.example.com", "/test"), ingress = Ingress(true))
        }

        val ingressRoute = context.createAndGetIngressRoute(flaisApplication)
        assertNotNull(ingressRoute)
        assertEquals("test", ingressRoute.metadata.name)
        assertEquals("web", ingressRoute.spec.entryPoints[0])
        assertEquals("Host(`test.example.com`) && PathPrefix(`/test`)", ingressRoute.spec.routes[0].match)
        assertEquals(8080, ingressRoute.spec.routes[0].services[0].port.intVal)
        assertEquals("test", ingressRoute.spec.routes[0].services[0].name)
        assertEquals(context.namespace, ingressRoute.spec.routes[0].services[0].namespace)
    }

    @Test
    fun `should not create IngressRoute since enabled is false`(context: KubernetesOperatorContext) {
        val flaisApplication = createTestFlaisApplication().apply {
            spec = spec.copy(url = Url("test.example.com", "/test"), ingress = Ingress(false))
        }

        val ingressRoute = context.createAndGetIngressRoute(flaisApplication)
        assertNull(ingressRoute)
    }

    @Test
    fun `should not create IngressRoute since url is not set`(context: KubernetesOperatorContext) {
        val flaisApplication = createTestFlaisApplication().apply {
            spec = spec.copy(ingress = Ingress(true))
        }

        val ingressRoute = context.createAndGetIngressRoute(flaisApplication)
        assertNull(ingressRoute)
    }
    //endregion

    //region Rules
    @Test
    fun `should not accept invalid hostname`(context: KubernetesOperatorContext) {
        val flaisApplication = createTestFlaisApplication().apply {
            spec = spec.copy(url = Url("notAValidHostname/&%", "/test"), ingress = Ingress(true))
        }

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
        val flaisApplication = createTestFlaisApplication().apply {
            spec = spec.copy(url = Url("test.example.com", "notAValidPath/&%=##dfnjkdkjn44"), ingress = Ingress(true))
        }

        try {
            context.create(flaisApplication)
        } catch (e: KubernetesClientException) {
            assertEquals(422, e.code)
            assertEquals("Invalid", e.status.reason)
            assert(e.status.message.contains("Invalid path"))
        }
    }
    //endregion

    //region RouteMatch
    @Test
    fun `should create IngressRoute with default path`(context: KubernetesOperatorContext) {
        val flaisApplication = createTestFlaisApplication().apply {
            spec = spec.copy(url = Url("test.example.com"), ingress = Ingress(true))
        }

        val ingressRoute = context.createAndGetIngressRoute(flaisApplication)
        assertNotNull(ingressRoute)
        assertEquals("Host(`test.example.com`)", ingressRoute.spec.routes[0].match)
    }

    @Test
    fun `should create IngressRoute with custom path`(context: KubernetesOperatorContext) {
        val flaisApplication = createTestFlaisApplication().apply {
            spec = spec.copy(url = Url("test.example.com", "/test"), ingress = Ingress(true))
        }

        val ingressRoute = context.createAndGetIngressRoute(flaisApplication)
        assertNotNull(ingressRoute)
        assertEquals("Host(`test.example.com`) && PathPrefix(`/test`)", ingressRoute.spec.routes[0].match)
    }
    //endregion

    private fun KubernetesOperatorContext.createAndGetIngressRoute(app: FlaisApplicationCrd) =
        createAndGetResource<IngressRoute>(app)


    companion object {
        @RegisterExtension
        val koinTestExtension = createKoinTestExtension()

        @RegisterExtension
        val kubernetesOperatorExtension = createKubernetesOperatorExtension()
    }
}