package no.fintlabs.operator.api.v1alpha1

data class Metrics(
    val enabled: Boolean = true,
    val path: String = "/actuator/prometheus",
    val port: String = "8080"
)