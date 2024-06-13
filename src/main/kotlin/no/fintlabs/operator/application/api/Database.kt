package no.fintlabs.operator.application.api

data class Database(val database: String? = null) {
    @Deprecated("Going to be removed") val enabled: Boolean = true
}
