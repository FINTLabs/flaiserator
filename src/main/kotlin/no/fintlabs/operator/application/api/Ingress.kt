package no.fintlabs.operator.application.api

import io.fabric8.generator.annotation.ValidationRule

data class Ingress(
    var enabled: Boolean = false,
    @ValidationRule(
        "self.matches('^/[A-Za-z0-9/_-]*$')",
        message = "Invalid path"
    )
    var basePath: String? = null,
    var middlewares: List<String> = emptyList()
)