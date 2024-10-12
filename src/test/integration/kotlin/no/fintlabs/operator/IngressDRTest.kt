package no.fintlabs.operator

import no.fintlabs.extensions.KubernetesOperatorContext
import no.fintlabs.operator.Utils.createAndGetResource
import no.fintlabs.operator.Utils.createKoinTestExtension
import no.fintlabs.operator.Utils.createKubernetesOperatorExtension
import no.fintlabs.operator.Utils.createTestFlaisApplication
import no.fintlabs.operator.api.v1alpha1.FlaisApplicationCrd
import no.fintlabs.operator.api.v1alpha1.Ingress
import org.junit.jupiter.api.extension.RegisterExtension
import us.containo.traefik.v1alpha1.IngressRoute
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class IngressDRTest{
    //region General
    @Test
    fun `should create simple IngressRoute`(context: KubernetesOperatorContext) {
        val flaisApplication = createTestFlaisApplication().apply {
            spec = spec.copy(ingress = Ingress(routes = listOf(
                Ingress.Route("test.example.com", "/test")
            )))
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
    fun `should create full IngressRoute`(context: KubernetesOperatorContext) {
        val flaisApplication = createTestFlaisApplication().apply {
            spec = spec.copy(ingress = Ingress(routes = listOf(
                Ingress.Route(
                    host = "test.example.com",
                    path = "/test",
                    queries = mapOf("key" to "value"),
                    headers = mapOf("header" to "value"),
                    middlewares = setOf("middleware_2")
                )
            ), middlewares = setOf("middleware_1")))
        }

        val ingressRoute = context.createAndGetIngressRoute(flaisApplication)
        assertNotNull(ingressRoute)
        assertEquals("test", ingressRoute.metadata.name)
        assertEquals("web", ingressRoute.spec.entryPoints[0])
        assertEquals("Host(`test.example.com`) && PathPrefix(`/test`) && Query(`key`, `value`) && Header(`header`, `value`)", ingressRoute.spec.routes[0].match)
        assertEquals(8080, ingressRoute.spec.routes[0].services[0].port.intVal)
        assertEquals("test", ingressRoute.spec.routes[0].services[0].name)
        assertEquals(context.namespace, ingressRoute.spec.routes[0].services[0].namespace)
        assertEquals("middleware_1", ingressRoute.spec.routes[0].middlewares[0].name)
        assertEquals(context.namespace, ingressRoute.spec.routes[0].middlewares[0].namespace)
        assertEquals("middleware_2", ingressRoute.spec.routes[0].middlewares[1].name)
        assertEquals(context.namespace, ingressRoute.spec.routes[0].middlewares[1].namespace)
    }

    @Test
    fun `should create IngressRoute with multiple routes`(context: KubernetesOperatorContext) {
        val flaisApplication = createTestFlaisApplication().apply {
            spec = spec.copy(ingress = Ingress(routes = listOf(
                Ingress.Route("test.example.com", "/test"),
                Ingress.Route("test2.example.com", "/test2")
            )))
        }

        val ingressRoute = context.createAndGetIngressRoute(flaisApplication)
        assertNotNull(ingressRoute)

        assertEquals("Host(`test.example.com`) && PathPrefix(`/test`)", ingressRoute.spec.routes[0].match)
        assertEquals(8080, ingressRoute.spec.routes[0].services[0].port.intVal)
        assertEquals("test", ingressRoute.spec.routes[0].services[0].name)
        assertEquals(context.namespace, ingressRoute.spec.routes[0].services[0].namespace)

        assertEquals("Host(`test2.example.com`) && PathPrefix(`/test2`)", ingressRoute.spec.routes[1].match)
        assertEquals(8080, ingressRoute.spec.routes[1].services[0].port.intVal)
        assertEquals("test", ingressRoute.spec.routes[1].services[0].name)
        assertEquals(context.namespace, ingressRoute.spec.routes[1].services[0].namespace)
    }

    @Test
    fun `should create IngressRoute with multiple middlewares`(context: KubernetesOperatorContext) {
        val flaisApplication = createTestFlaisApplication().apply {
            spec = spec.copy(ingress = Ingress(routes = listOf(
                Ingress.Route("test.example.com", "/test", middlewares = setOf("middleware_2", "middleware_3"))
            ), middlewares = setOf("middleware_1")))
        }

        val ingressRoute = context.createAndGetIngressRoute(flaisApplication)
        assertNotNull(ingressRoute)

        assertEquals("middleware_1", ingressRoute.spec.routes[0].middlewares[0].name)
        assertEquals(context.namespace, ingressRoute.spec.routes[0].middlewares[0].namespace)
        assertEquals("middleware_2", ingressRoute.spec.routes[0].middlewares[1].name)
        assertEquals(context.namespace, ingressRoute.spec.routes[0].middlewares[1].namespace)
        assertEquals("middleware_3", ingressRoute.spec.routes[0].middlewares[2].name)
        assertEquals(context.namespace, ingressRoute.spec.routes[0].middlewares[2].namespace)
    }

    @Test
    fun `should not create IngressRoute`(context: KubernetesOperatorContext) {
        val flaisApplication = createTestFlaisApplication()

        val ingressRoute = context.createAndGetIngressRoute(flaisApplication)
        assertNull(ingressRoute)
    }
    //endregion

    //region Rules
    @Test
    fun `should create IngressRoute with multiple queries`(context: KubernetesOperatorContext) {
        val flaisApplication = createTestFlaisApplication().apply {
            spec = spec.copy(ingress = Ingress(routes = listOf(
                Ingress.Route("test.example.com", "/test", queries = mapOf("key" to "value", "key2" to "value2"))
            )))
        }

        val ingressRoute = context.createAndGetIngressRoute(flaisApplication)
        assertNotNull(ingressRoute)
        assertEquals("Host(`test.example.com`) && PathPrefix(`/test`) && Query(`key`, `value`) && Query(`key2`, `value2`)", ingressRoute.spec.routes[0].match)
    }

    @Test
    fun `should create IngressRoute with multiple headers`(context: KubernetesOperatorContext) {
        val flaisApplication = createTestFlaisApplication().apply {
            spec = spec.copy(ingress = Ingress(routes = listOf(
                Ingress.Route("test.example.com", "/test", headers = mapOf("header" to "value", "header2" to "value2"))
            )))
        }

        val ingressRoute = context.createAndGetIngressRoute(flaisApplication)
        assertNotNull(ingressRoute)
        assertEquals("Host(`test.example.com`) && PathPrefix(`/test`) && Header(`header`, `value`) && Header(`header2`, `value2`)", ingressRoute.spec.routes[0].match)
    }

    @Test
    fun `should create IngressRoute with regex path`(context: KubernetesOperatorContext) {
        val flaisApplication = createTestFlaisApplication().apply {
            spec = spec.copy(ingress = Ingress(routes = listOf(
                Ingress.Route("test.example.com", "re:/test.*")
            )))
        }

        val ingressRoute = context.createAndGetIngressRoute(flaisApplication)
        assertNotNull(ingressRoute)
        assertEquals("Host(`test.example.com`) && PathRegexp(`/test.*`)", ingressRoute.spec.routes[0].match)
    }

    @Test
    fun `should create IngressRoute with regex query`(context: KubernetesOperatorContext) {
        val flaisApplication = createTestFlaisApplication().apply {
            spec = spec.copy(ingress = Ingress(routes = listOf(
                Ingress.Route("test.example.com", "/test", queries = mapOf("key" to "re:value.*"))
            )))
        }

        val ingressRoute = context.createAndGetIngressRoute(flaisApplication)
        assertNotNull(ingressRoute)
        assertEquals("Host(`test.example.com`) && PathPrefix(`/test`) && QueryRegexp(`key`, `value.*`)", ingressRoute.spec.routes[0].match)
    }

    @Test
    fun `should create IngressRoute with regex header`(context: KubernetesOperatorContext) {
        val flaisApplication = createTestFlaisApplication().apply {
            spec = spec.copy(ingress = Ingress(routes = listOf(
                Ingress.Route("test.example.com", "/test", headers = mapOf("header" to "re:value.*"))
            )))
        }

        val ingressRoute = context.createAndGetIngressRoute(flaisApplication)
        assertNotNull(ingressRoute)
        assertEquals("Host(`test.example.com`) && PathPrefix(`/test`) && HeaderRegexp(`header`, `value.*`)", ingressRoute.spec.routes[0].match)
    }

    @Test
    fun `should create IngressRoute with multiple regexes`(context: KubernetesOperatorContext) {
        val flaisApplication = createTestFlaisApplication().apply {
            spec = spec.copy(ingress = Ingress(routes = listOf(
                Ingress.Route("test.example.com", "re:/test.*", queries = mapOf("key" to "re:value.*"), headers = mapOf("header" to "re:value.*"))
            )))
        }

        val ingressRoute = context.createAndGetIngressRoute(flaisApplication)
        assertNotNull(ingressRoute)
        assertEquals("Host(`test.example.com`) && PathRegexp(`/test.*`) && QueryRegexp(`key`, `value.*`) && HeaderRegexp(`header`, `value.*`)", ingressRoute.spec.routes[0].match)
    }

    @Test
    fun `should create IngressRoute with multiple regexes and non-regexes`(context: KubernetesOperatorContext) {
        val flaisApplication = createTestFlaisApplication().apply {
            spec = spec.copy(ingress = Ingress(routes = listOf(
                Ingress.Route("test.example.com", "/test", queries = mapOf("key" to "re:value.*", "key2" to "value2"), headers = mapOf("header" to "re:value.*", "header2" to "value2"))
            )))
        }

        val ingressRoute = context.createAndGetIngressRoute(flaisApplication)
        assertNotNull(ingressRoute)
        assertEquals("Host(`test.example.com`) && PathPrefix(`/test`) && QueryRegexp(`key`, `value.*`) && Query(`key2`, `value2`) && HeaderRegexp(`header`, `value.*`) && Header(`header2`, `value2`)", ingressRoute.spec.routes[0].match)
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