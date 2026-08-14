package no.fintlabs.application.api.v1alpha1

import io.fabric8.generator.annotation.ValidationRule

data class AutoInstrumentation(
    val enabled: Boolean = false,
    @ValidationRule(
        "self in ['java', 'nodejs', 'python', 'dotnet', 'sdk']",
        message = "Invalid instrumentation runtime, must be one of java, nodejs, python, dotnet, sdk",
    )
    val runtime: String? = null,
)
