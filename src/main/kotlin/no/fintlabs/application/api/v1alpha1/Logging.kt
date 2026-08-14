package no.fintlabs.application.api.v1alpha1

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonValue

enum class LogDestination(
    @JsonValue private val value: String,
) {
    LOKI("loki"),
    ;

    override fun toString(): String = value

    companion object {
        @JvmStatic
        @JsonCreator
        fun fromValue(value: String): LogDestination =
            entries.firstOrNull { it.value.equals(value, ignoreCase = true) }
                ?: throw IllegalArgumentException("Unknown log destination: $value")
    }
}

data class Logging(
    @Deprecated("Use destination instead")
    val loki: Boolean? = null,
    val enabled: Boolean? = null,
    val destination: LogDestination = LogDestination.LOKI,
    val otel: Boolean = false,
)
