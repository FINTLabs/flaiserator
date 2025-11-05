package no.fintlabs.application

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import no.fintlabs.application.Utils.createAndGetResource
import no.fintlabs.application.Utils.createKoinTestExtension
import no.fintlabs.application.Utils.createKubernetesOperatorExtension
import no.fintlabs.application.Utils.createTestFlaisApplication
import no.fintlabs.application.api.v1alpha1.FlaisApplication
import no.fintlabs.application.api.v1alpha1.Ingress
import no.fintlabs.extensions.KubernetesOperatorContext
import org.junit.jupiter.api.extension.RegisterExtension
import us.containo.traefik.v1alpha1.IngressRoute

class IngressDRTest {
  // region General
  @Test
  fun `should create simple IngressRoute`(context: KubernetesOperatorContext) {
    val flaisApplication =
        createTestFlaisApplication().apply {
          spec =
              spec.copy(
                  ingress = Ingress(routes = listOf(Ingress.Route("test.example.com", "/test")))
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
    assertEquals(8080, ingressRoute.spec.routes[0].services[0].port.intVal)
    assertEquals("test", ingressRoute.spec.routes[0].services[0].name)
    assertEquals(context.namespace, ingressRoute.spec.routes[0].services[0].namespace)
  }

  @Test
  fun `should create full IngressRoute`(context: KubernetesOperatorContext) {
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
                                      queries = mapOf("key" to "value"),
                                      headers = mapOf("header" to "value"),
                                      middlewares = setOf("middleware_2"),
                                  )
                              ),
                          middlewares = setOf("middleware_1"),
                      )
              )
        }

    val ingressRoute = context.createAndGetIngressRoute(flaisApplication)
    assertNotNull(ingressRoute)
    assertEquals("test", ingressRoute.metadata.name)
    assertEquals("web", ingressRoute.spec.entryPoints[0])
    assertEquals(
        "Host(`test.example.com`) && PathPrefix(`/test`) && Query(`key=value`) && Headers(`header`, `value`)",
        ingressRoute.spec.routes[0].match,
    )
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
    val flaisApplication =
        createTestFlaisApplication().apply {
          spec =
              spec.copy(
                  ingress =
                      Ingress(
                          routes =
                              listOf(
                                  Ingress.Route("test.example.com", "/test"),
                                  Ingress.Route("test2.example.com", "/test2"),
                              )
                      )
              )
        }

    val ingressRoute = context.createAndGetIngressRoute(flaisApplication)
    assertNotNull(ingressRoute)

    assertEquals(
        "Host(`test.example.com`) && PathPrefix(`/test`)",
        ingressRoute.spec.routes[0].match,
    )
    assertEquals(8080, ingressRoute.spec.routes[0].services[0].port.intVal)
    assertEquals("test", ingressRoute.spec.routes[0].services[0].name)
    assertEquals(context.namespace, ingressRoute.spec.routes[0].services[0].namespace)

    assertEquals(
        "Host(`test2.example.com`) && PathPrefix(`/test2`)",
        ingressRoute.spec.routes[1].match,
    )
    assertEquals(8080, ingressRoute.spec.routes[1].services[0].port.intVal)
    assertEquals("test", ingressRoute.spec.routes[1].services[0].name)
    assertEquals(context.namespace, ingressRoute.spec.routes[1].services[0].namespace)
  }

  @Test
  fun `should create IngressRoute with multiple middlewares`(context: KubernetesOperatorContext) {
    val flaisApplication =
        createTestFlaisApplication().apply {
          spec =
              spec.copy(
                  ingress =
                      Ingress(
                          routes =
                              listOf(
                                  Ingress.Route(
                                      "test.example.com",
                                      "/test",
                                      middlewares = setOf("middleware_2", "middleware_3"),
                                  )
                              ),
                          middlewares = setOf("middleware_1"),
                      )
              )
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

  // endregion

  // region Rules
  @Test
  fun `should create IngressRoute with multiple queries`(context: KubernetesOperatorContext) {
    val flaisApplication =
        createTestFlaisApplication().apply {
          spec =
              spec.copy(
                  ingress =
                      Ingress(
                          routes =
                              listOf(
                                  Ingress.Route(
                                      "test.example.com",
                                      "/test",
                                      queries = mapOf("key" to "value", "key2" to "value2"),
                                  )
                              )
                      )
              )
        }

    val ingressRoute = context.createAndGetIngressRoute(flaisApplication)
    assertNotNull(ingressRoute)
    assertEquals(
        "Host(`test.example.com`) && PathPrefix(`/test`) && Query(`key=value`, `key2=value2`)",
        ingressRoute.spec.routes[0].match,
    )
  }

  @Test
  fun `should create IngressRoute with multiple headers`(context: KubernetesOperatorContext) {
    val flaisApplication =
        createTestFlaisApplication().apply {
          spec =
              spec.copy(
                  ingress =
                      Ingress(
                          routes =
                              listOf(
                                  Ingress.Route(
                                      "test.example.com",
                                      "/test",
                                      headers = mapOf("header" to "value", "header2" to "value2"),
                                  )
                              )
                      )
              )
        }

    val ingressRoute = context.createAndGetIngressRoute(flaisApplication)
    assertNotNull(ingressRoute)
    assertEquals(
        "Host(`test.example.com`) && PathPrefix(`/test`) && Headers(`header`, `value`) && Headers(`header2`, `value2`)",
        ingressRoute.spec.routes[0].match,
    )
  }

  @Test
  fun `should create IngressRoute with regex path`(context: KubernetesOperatorContext) {
    val flaisApplication =
        createTestFlaisApplication().apply {
          spec =
              spec.copy(
                  ingress =
                      Ingress(routes = listOf(Ingress.Route("test.example.com", "/{path:test.*}")))
              )
        }

    val ingressRoute = context.createAndGetIngressRoute(flaisApplication)
    assertNotNull(ingressRoute)
    assertEquals(
        "Host(`test.example.com`) && PathPrefix(`/{path:test.*}`)",
        ingressRoute.spec.routes[0].match,
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
                                      "test.example.com",
                                      "/test",
                                      headers = mapOf("header" to "re:value.*"),
                                  )
                              )
                      )
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
  fun `should create IngressRoute with multiple regexes`(context: KubernetesOperatorContext) {
    val flaisApplication =
        createTestFlaisApplication().apply {
          spec =
              spec.copy(
                  ingress =
                      Ingress(
                          routes =
                              listOf(
                                  Ingress.Route(
                                      "test.example.com",
                                      "/{name:test.*}",
                                      queries = mapOf("key" to "value"),
                                      headers = mapOf("header" to "re:value.*"),
                                  )
                              )
                      )
              )
        }

    val ingressRoute = context.createAndGetIngressRoute(flaisApplication)
    assertNotNull(ingressRoute)
    assertEquals(
        "Host(`test.example.com`) && PathPrefix(`/{name:test.*}`) && Query(`key=value`) && HeadersRegexp(`header`, `value.*`)",
        ingressRoute.spec.routes[0].match,
    )
  }

  @Test
  fun `should create IngressRoute with multiple regexes and non-regexes`(
      context: KubernetesOperatorContext
  ) {
    val flaisApplication =
        createTestFlaisApplication().apply {
          spec =
              spec.copy(
                  ingress =
                      Ingress(
                          routes =
                              listOf(
                                  Ingress.Route(
                                      "test.example.com",
                                      "/test",
                                      queries = mapOf("key" to "value", "key2" to "value2"),
                                      headers =
                                          mapOf("header" to "re:value.*", "header2" to "value2"),
                                  )
                              )
                      )
              )
        }

    val ingressRoute = context.createAndGetIngressRoute(flaisApplication)
    assertNotNull(ingressRoute)
    assertEquals(
        "Host(`test.example.com`) && PathPrefix(`/test`) && Query(`key=value`, `key2=value2`) && HeadersRegexp(`header`, `value.*`) && Headers(`header2`, `value2`)",
        ingressRoute.spec.routes[0].match,
    )
  }

  // endregion

  private fun KubernetesOperatorContext.createAndGetIngressRoute(app: FlaisApplication) =
      createAndGetResource<IngressRoute>(app)

  companion object {
    @RegisterExtension val koinTestExtension = createKoinTestExtension()

    @RegisterExtension val kubernetesOperatorExtension = createKubernetesOperatorExtension()
  }
}
