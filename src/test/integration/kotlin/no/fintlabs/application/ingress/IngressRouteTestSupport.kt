package no.fintlabs.application.ingress

import io.fabric8.kubernetes.api.model.IntOrString
import io.fabric8.kubernetes.api.model.ObjectMeta
import io.traefik.v1alpha1.IngressRouteSpec
import io.traefik.v1alpha1.ingressroutespec.Routes
import io.traefik.v1alpha1.ingressroutespec.routes.Services
import no.fintlabs.application.Utils.createAndGetResource
import no.fintlabs.application.Utils.createTestFlaisApplication
import no.fintlabs.application.api.v1alpha1.FlaisApplication
import no.fintlabs.application.api.v1alpha1.Ingress
import no.fintlabs.application.api.v1alpha1.Url
import no.fintlabs.extensions.KubernetesOperatorContext
import kotlin.test.assertEquals
import io.traefik.v1alpha1.IngressRoute as V3IngressRoute
import us.containo.traefik.v1alpha1.IngressRoute as LegacyIngressRoute

data class IngressRouteView(
    val name: String,
    val entryPoints: List<String>,
    val routes: List<IngressRouteRouteView>,
)

data class IngressRouteRouteView(
    val match: String,
    val services: List<IngressRouteServiceView>,
    val middlewares: List<IngressRouteMiddlewareView>,
)

data class IngressRouteServiceView(
    val name: String,
    val namespace: String?,
    val port: Int?,
)

data class IngressRouteMiddlewareView(
    val name: String,
    val namespace: String?,
)

fun LegacyIngressRoute.toIngressRouteView() =
    IngressRouteView(
        name = metadata.name,
        entryPoints = spec.entryPoints,
        routes =
            spec.routes.map { route ->
                IngressRouteRouteView(
                    match = route.match,
                    services =
                        route.services.map { service ->
                            IngressRouteServiceView(
                                name = service.name,
                                namespace = service.namespace,
                                port = service.port.intVal,
                            )
                        },
                    middlewares =
                        route.middlewares.orEmpty().map { middleware ->
                            IngressRouteMiddlewareView(
                                name = middleware.name,
                                namespace = middleware.namespace,
                            )
                        },
                )
            },
    )

fun V3IngressRoute.toIngressRouteView() =
    IngressRouteView(
        name = metadata.name,
        entryPoints = spec.entryPoints,
        routes =
            spec.routes.map { route ->
                IngressRouteRouteView(
                    match = route.match,
                    services =
                        route.services.map { service ->
                            IngressRouteServiceView(
                                name = service.name,
                                namespace = service.namespace,
                                port = service.port.intVal,
                            )
                        },
                    middlewares =
                        route.middlewares.orEmpty().map { middleware ->
                            IngressRouteMiddlewareView(
                                name = middleware.name,
                                namespace = middleware.namespace,
                            )
                        },
                )
            },
    )

fun KubernetesOperatorContext.createAndGetLegacyIngressRoute(app: FlaisApplication) = createAndGetResource<LegacyIngressRoute>(app)

fun KubernetesOperatorContext.createAndGetV3IngressRoute(app: FlaisApplication) = createAndGetResource<V3IngressRoute>(app)

fun simpleIngressApplication() =
    createTestFlaisApplication().apply {
        spec =
            spec.copy(
                ingress = Ingress(routes = listOf(Ingress.Route("test.example.com", "/test"))),
            )
    }

fun legacyIngressApplication(
    enabled: Boolean = true,
    host: String? = "test.example.com",
    path: String? = "/test",
) = createTestFlaisApplication().apply {
    spec = spec.copy(ingress = Ingress(enabled))
    host?.let {
        spec = spec.copy(url = Url(it, path))
    }
}

fun KubernetesOperatorContext.unmanagedV3IngressRoute(app: FlaisApplication) =
    V3IngressRoute().apply {
        metadata =
            ObjectMeta().apply {
                name = app.metadata.name
                namespace = this@unmanagedV3IngressRoute.namespace
            }
        spec =
            IngressRouteSpec().apply {
                entryPoints = listOf("web")
                routes =
                    listOf(
                        Routes().apply {
                            kind = Routes.Kind.RULE
                            match = "Host(`stale.example.com`)"
                            services =
                                listOf(
                                    Services().apply {
                                        name = "stale"
                                        port = IntOrString(80)
                                    },
                                )
                        },
                    )
            }
    }

fun assertSimpleIngressRoute(
    ingressRoute: IngressRouteView,
    namespace: String,
) {
    assertEquals("test", ingressRoute.name)
    assertEquals("web", ingressRoute.entryPoints[0])
    assertRoute(
        ingressRoute.routes[0],
        namespace,
        "Host(`test.example.com`) && PathPrefix(`/test`)",
    )
}

fun assertRoute(
    route: IngressRouteRouteView,
    namespace: String,
    match: String,
) {
    assertEquals(match, route.match)
    assertEquals(8080, route.services[0].port)
    assertEquals("test", route.services[0].name)
    assertEquals(namespace, route.services[0].namespace)
}
