package no.fintlabs.operator.application

import io.fabric8.kubernetes.client.KubernetesClientException
import no.fintlabs.baseModule
import no.fintlabs.operator.KubernetesOperatorContext
import no.fintlabs.operator.KubernetesOperatorExtension
import no.fintlabs.operator.application.Utils.createAndGetResource
import no.fintlabs.operator.application.Utils.createTestFlaisApplication
import no.fintlabs.operator.application.api.FlaisApplicationCrd
import no.fintlabs.operator.application.api.Ingress
import no.fintlabs.operator.application.api.Url
import org.junit.jupiter.api.extension.RegisterExtension
import org.koin.test.junit5.KoinTestExtension
import us.containo.traefik.v1alpha1.IngressRoute
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class IngressDependentResourceTest{
    //region General
    @Test
    fun `should creaet IngressRoute`(context: KubernetesOperatorContext) {
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
        @JvmField
        @RegisterExtension
        val koinTestExtension = KoinTestExtension.create {
            modules(baseModule, applicationModule())
        }

        @JvmField
        @RegisterExtension
        val kubernetesOperatorExtension = KubernetesOperatorExtension.create(
            listOf(IngressRoute::class.java)
        )
    }
}