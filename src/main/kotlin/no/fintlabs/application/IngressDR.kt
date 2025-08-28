package no.fintlabs.application

import io.fabric8.kubernetes.api.model.IntOrString
import io.javaoperatorsdk.operator.api.config.informer.Informer
import io.javaoperatorsdk.operator.api.reconciler.Context
import io.javaoperatorsdk.operator.processing.dependent.kubernetes.CRUDKubernetesDependentResource
import io.javaoperatorsdk.operator.processing.dependent.kubernetes.KubernetesDependent
import no.fintlabs.application.api.MANAGED_BY_FLAISERATOR_SELECTOR
import no.fintlabs.application.api.v1alpha1.FlaisApplicationCrd
import no.fintlabs.application.api.v1alpha1.Ingress.Route
import no.fintlabs.application.api.v1alpha1.isIngressEnabled
import no.fintlabs.application.api.v1alpha1.isLegacy
import no.fintlabs.operator.dependent.ReconcileCondition
import us.containo.traefik.v1alpha1.IngressRoute
import us.containo.traefik.v1alpha1.IngressRouteSpec
import us.containo.traefik.v1alpha1.ingressroutespec.Routes
import us.containo.traefik.v1alpha1.ingressroutespec.routes.Middlewares
import us.containo.traefik.v1alpha1.ingressroutespec.routes.Services

@KubernetesDependent(informer = Informer(labelSelector = MANAGED_BY_FLAISERATOR_SELECTOR))
class IngressDR :
    CRUDKubernetesDependentResource<IngressRoute, FlaisApplicationCrd>(IngressRoute::class.java),
    ReconcileCondition<FlaisApplicationCrd> {
  override fun name(): String = "ingress"

  override fun desired(primary: FlaisApplicationCrd, context: Context<FlaisApplicationCrd>) =
      IngressRoute().apply {
        metadata = createObjectMeta(primary)
        spec =
            IngressRouteSpec().apply {
              entryPoints = listOf("web")
              routes =
                  primary.spec.ingress!!.let { ingress ->
                    if (ingress.isLegacy()) listOf(createLegacyAppRoute(primary))
                    else ingress.routes!!.map { createAppRoute(it, primary) }
                  }
            }
      }

  private fun createAppRoute(route: Route, primary: FlaisApplicationCrd) =
      Routes().apply {
        kind = Routes.Kind.RULE
        match = createMatch(route)
        services =
            listOf(
                Services().apply {
                  port = IntOrString(primary.spec.port)
                  name = primary.metadata.name
                  namespace = primary.metadata.namespace
                }
            )
        middlewares =
            route.allMiddlewares
                .map {
                  Middlewares().apply {
                    name = it
                    namespace = primary.metadata.namespace
                  }
                }
                .ifEmpty { null }
      }

  private fun createMatch(route: Route) = buildString {
    append("Host(`${route.host}`)")
    route.path
        ?.takeIf { it.isNotEmpty() }
        ?.let {
          append(" && ")
          append("PathPrefix(`$it`)")
        }
    route.queries
        ?.filter { it.key.isNotEmpty() && it.value.isNotEmpty() }
        ?.map { (key, value) -> "`$key=$value`" }
        ?.let {
          append(" && ")
          append("Query(${it.joinToString(", ")})")
        }
    route.headers
        ?.filter { it.key.isNotEmpty() && it.value.isNotEmpty() }
        ?.forEach { (key, value) ->
          append(" && ")
          append(
              if (value.isRegex()) "HeadersRegexp(`$key`, `${value.stripRegexPrefix().toRegex()}`)"
              else "Headers(`$key`, `$value`)"
          )
        }
  }

  private fun String.isRegex() = this.startsWith("re:")

  private fun String.stripRegexPrefix() = this.removePrefix("re:")

  // region Legacy
  private fun createLegacyAppRoute(primary: FlaisApplicationCrd) =
      Routes().apply {
        kind = Routes.Kind.RULE
        match = createLegacyMatch(primary)
        services =
            listOf(
                Services().apply {
                  port = IntOrString(primary.spec.port)
                  name = primary.metadata.name
                  namespace = primary.metadata.namespace
                }
            )
        middlewares =
            primary.spec.ingress?.middlewares?.map {
              Middlewares().apply {
                name = it
                namespace = primary.metadata.namespace
              }
            }
      }

  private fun createLegacyMatch(primary: FlaisApplicationCrd) =
      listOfNotNull(
              "Host(`${primary.spec.url.hostname}`)",
              legacyBasePath(primary).takeUnless { it.isEmpty() }?.let { "PathPrefix(`$it`)" },
          )
          .joinToString(" && ")

  private fun legacyBasePath(primary: FlaisApplicationCrd) =
      primary.spec.ingress?.basePath.takeUnless { it.isNullOrEmpty() }
          ?: primary.spec.url.basePath.orEmpty()

  override fun shouldReconcile(
      primary: FlaisApplicationCrd,
      context: Context<FlaisApplicationCrd>,
  ): Boolean {
    return primary.spec.isIngressEnabled()
  }
  // endregion
}
