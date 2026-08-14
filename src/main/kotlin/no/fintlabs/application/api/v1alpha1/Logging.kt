package no.fintlabs.application.api.v1alpha1

enum class LogDestination {
    LOKI,
}

data class Logging(
    @Deprecated("Use destination instead")
    val loki: Boolean? = null,
    val enabled: Boolean? = null,
    val destination: LogDestination = LogDestination.LOKI,
    val otel: Boolean = false,
)
