package no.fintlabs.operator.application.api

import io.fabric8.generator.annotation.ValidationRule

data class Url(
    @ValidationRule(
        "self.matches('^[a-zA-Z0-9]([a-zA-Z0-9-]{0,251}[a-zA-Z0-9])?$')",
        message = "Invalid hostname"
    )
    var hostname: String? = null,

    @ValidationRule(
        "self.matches('^/[A-Za-z0-9/_-]*$')",
        message = "Invalid path"
    )
    var basePath: String? = null,
)
