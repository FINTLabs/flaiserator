package no.fintlabs.application.api.v1alpha1

data class Metrics(
    val enabled: Boolean = true,
    val path: String = "/actuator/prometheus",
    val port: String = "8080",
    val otel: Boolean = false,
)

data class Prometheus(
    val enabled: Boolean = true,
    val path: String = "/actuator/prometheus",
    val port: String = "8080",
)

fun Prometheus.toMetrics() =
    Metrics(
        enabled = enabled,
        path = path,
        port = port,
    )
