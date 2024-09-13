package no.fintlabs.operator.application

import io.fabric8.kubernetes.api.model.IntOrString
import io.javaoperatorsdk.operator.api.reconciler.Context
import io.javaoperatorsdk.operator.processing.dependent.kubernetes.CRUDKubernetesDependentResource
import io.javaoperatorsdk.operator.processing.dependent.kubernetes.KubernetesDependent
import no.fintlabs.operator.application.api.FlaisApplicationCrd
import no.fintlabs.operator.application.api.MANAGED_BY_FLAISERATOR_SELECTOR
import us.containo.traefik.v1alpha1.IngressRoute
import us.containo.traefik.v1alpha1.IngressRouteSpec
import us.containo.traefik.v1alpha1.ingressroutespec.Routes
import us.containo.traefik.v1alpha1.ingressroutespec.routes.Middlewares
import us.containo.traefik.v1alpha1.ingressroutespec.routes.Services

@KubernetesDependent(
    labelSelector = MANAGED_BY_FLAISERATOR_SELECTOR
)
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

    private fun createMatch(primary: FlaisApplicationCrd) = listOfNotNull(
        "Host(`${primary.spec.url.hostname}`)",
        createBasePaths(primary),
        createHeaders(primary.spec.ingress.headers)
    ).joinToString(" && ")

    private fun createBasePaths(primary: FlaisApplicationCrd): String? {
        val basePaths = listOfNotNull(
            basePath(primary).takeUnless { it.isEmpty() }?.let { "PathPrefix(`$it`)" },
            *primary.spec.ingress.basePaths.takeUnless { it.isEmpty() }
                ?.map { "PathPrefix(`$it`)" }
                ?.toTypedArray() ?: emptyArray()
        )

        return basePaths.takeIf { it.isNotEmpty() }
            ?.joinToString(" || ")
            ?.let { if (basePaths.size > 1) "($it)" else it }
    }

    private fun createHeaders(headers: Map<String, String>?): String? {
        return null
    }

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