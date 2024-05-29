package no.fintlabs.operator.application

import io.fabric8.kubernetes.api.model.IntOrString
import io.javaoperatorsdk.operator.api.reconciler.Context
import io.javaoperatorsdk.operator.processing.dependent.kubernetes.CRUDKubernetesDependentResource
import no.fintlabs.operator.application.api.FlaisApplicationCrd
import us.containo.traefik.v1alpha1.IngressRoute
import us.containo.traefik.v1alpha1.IngressRouteSpec
import us.containo.traefik.v1alpha1.ingressroutespec.Routes
import us.containo.traefik.v1alpha1.ingressroutespec.routes.Middlewares
import us.containo.traefik.v1alpha1.ingressroutespec.routes.Services

class IngressDR : CRUDKubernetesDependentResource<IngressRoute, FlaisApplicationCrd>(IngressRoute::class.java) {
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

    private fun createMatch(primary: FlaisApplicationCrd): String {
        val pathConditions = getPathConditions(primary)
        val headerConditions = getHeaderConditions(primary)
        return listOfNotNull(
            getHostCondition(primary),
            pathConditions.takeUnless { it.isEmpty() },
            headerConditions.takeUnless { it.isEmpty() }
        ).joinToString(" && ")
    }

    private fun getPathConditions(primary: FlaisApplicationCrd): String {
        val paths = primary.spec.ingress.basePaths.takeUnless { it.isNullOrEmpty() }
            ?: listOf(primary.spec.ingress.basePath ?: primary.spec.url.basePath.orEmpty())
        return paths.filterNot { it.isEmpty() }.joinToString(" || ") { "PathPrefix(`$it`)" }
    }

    private fun getHeaderConditions(primary: FlaisApplicationCrd): String {
        return primary.spec.ingress.headers.entries.joinToString(" && ") {
            "Headers(`${it.key}`, `${it.value}`)"
        }
    }

    private fun getHostCondition(primary: FlaisApplicationCrd): String {
        return "Host(`${primary.spec.url.hostname}`)"
    }

    private fun createMiddlewares(primary: FlaisApplicationCrd) = primary.spec.ingress.middlewares.map {
        Middlewares().apply {
            name = it
            namespace = primary.metadata.namespace
        }
    }

    companion object {
        const val COMPONENT = "ingress"
    }
}
