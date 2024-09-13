package no.fintlabs.operator.application.api

import io.fabric8.generator.annotation.ValidationRule

data class Ingress(
    val enabled: Boolean = false,
    @ValidationRule(
        "self.matches('^/[A-Za-z0-9/_-]*$')",
        message = "Invalid path"
    )
    val basePath: String? = null,
    val basePaths: List<String> = emptyList(),
    val headers: Map<String, String> = emptyMap(),
    val middlewares: List<String> = emptyList(),
)