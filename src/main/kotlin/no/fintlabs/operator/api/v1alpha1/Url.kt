package no.fintlabs.operator.api.v1alpha1

data class Url(
    @Deprecated("Not used in future versions")
    val hostname: String? = null,

    @Deprecated("Not used in future versions")
    val basePath: String? = null,
)
