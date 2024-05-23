package no.fintlabs.operator.application.api

data class Prometheus(
    val enabled: Boolean = true,
    val path: String = "/actuator/prometheus",
    val port: String = "8080"
)