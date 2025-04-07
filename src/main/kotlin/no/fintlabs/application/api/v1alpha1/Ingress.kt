package no.fintlabs.application.api.v1alpha1

import com.fasterxml.jackson.annotation.JsonIgnore
import io.fabric8.generator.annotation.Required
import io.fabric8.generator.annotation.ValidationRule

data class Ingress(
    @Deprecated("Not used in future versions. Use routes instead")
    val enabled: Boolean = false,

    @Deprecated("Not used in future versions. Use routes instead")
    @ValidationRule(
        "self.matches('^/[A-Za-z0-9/_-]*$')",
        message = "Invalid path"
    )
    val basePath: String? = null,

    val routes: List<Route>? = null,
    val middlewares: Set<String>? = null,
) {
    init {
        routes?.forEach { it.allMiddlewares = middlewares.orEmpty() + it.middlewares.orEmpty() }
    }

    data class Route(
        @Required
        val host: String,
        val path: String? = null,

        val headers: Map<String, String>? = null,
        val queries: Map<String, String>? = null,

        var middlewares: Set<String>? = null,
    ) {
        @JsonIgnore
        lateinit var allMiddlewares: Set<String>
    }
}

fun Ingress.isLegacy(): Boolean = this.routes.isNullOrEmpty()

fun FlaisApplicationSpec.isIngressEnabled() = when {
    ingress == null -> false
    ingress.isLegacy() -> ingress.enabled && !url.hostname.isNullOrEmpty()
    else -> true
}