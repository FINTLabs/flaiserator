package no.fintlabs.operator.api.v1alpha1

data class Observability(
    val metrics: Metrics? = null,
    val logging: Logging? = null
)