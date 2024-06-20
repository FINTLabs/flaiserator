package no.fintlabs.operator.application

import io.fabric8.kubernetes.api.model.IntOrString
import io.javaoperatorsdk.operator.api.reconciler.Context
import io.javaoperatorsdk.operator.processing.dependent.kubernetes.CRUDKubernetesDependentResource
import io.javaoperatorsdk.operator.processing.dependent.kubernetes.KubernetesDependent
import no.fintlabs.operator.application.api.FlaisApplicationCrd
import us.containo.traefik.v1alpha1.IngressRoute
import us.containo.traefik.v1alpha1.IngressRouteSpec
import us.containo.traefik.v1alpha1.ingressroutespec.Routes
import us.containo.traefik.v1alpha1.ingressroutespec.routes.Middlewares
import us.containo.traefik.v1alpha1.ingressroutespec.routes.Services

@KubernetesDependent
class IngressDR : CRUDKubernetesDependentResource<IngressRoute, FlaisApplicationCrd>(IngressRoute::class.java)  {
    override fun desired(primary: FlaisApplicationCrd, context: Context<FlaisApplicationCrd>) = IngressRoute().apply {
        metadata = createObjectMeta(primary)
        spec = IngressRouteSpec().apply {
            entryPoints = listOf("web")
            routes = listOf(createAppRoute(primary))
        }
    }

    private fun createAppRoute(primary: FlaisApplicationCrd) = Routes().apply {
        kind = Routes.Kind.RULE
        match = createMatch(primary)
        services = listOf(
            Services().apply {
                port = IntOrString(primary.spec.port)
                name = primary.metadata.name
                namespace = primary.metadata.namespace
            }
        )
        middlewares = createMiddlewares(primary)
    }

    private fun createMatch(primary: FlaisApplicationCrd) = listOfNotNull(
        "Host(`${primary.spec.url.hostname}`)",
        basePath(primary).takeUnless { it.isEmpty() }?.let { "PathPrefix(`$it`)" }
    ).joinToString(" && ")

    private fun createMiddlewares(primary: FlaisApplicationCrd) = primary.spec.ingress.middlewares.map {
        Middlewares().apply {
            name = it
            namespace = primary.metadata.namespace
        }
    }

    private fun basePath(primary: FlaisApplicationCrd) =
        primary.spec.ingress.basePath.takeUnless { it.isNullOrEmpty() } ?: primary.spec.url.basePath.orEmpty()

    companion object {
        const val COMPONENT = "ingress"
    }
}