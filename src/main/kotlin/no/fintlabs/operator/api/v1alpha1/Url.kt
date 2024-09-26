package no.fintlabs.operator.api.v1alpha1

import io.fabric8.generator.annotation.ValidationRule

data class Url(
    @Deprecated("Not used in future versions")
    @ValidationRule(
        "self.matches('^[a-zA-Z0-9]([-a-zA-Z0-9]*[a-zA-Z0-9])?(\\\\.[a-zA-Z0-9]([-a-zA-Z0-9]*[a-zA-Z0-9])?)*\$')",
        message = "Invalid hostname"
    )
    val hostname: String? = null,

    @Deprecated("Not used in future versions")
    @ValidationRule(
        "self.matches('^/[A-Za-z0-9/_-]*$')",
        message = "Invalid path"
    )
    val basePath: String? = null,
)
