package no.fintlabs.operator.application.api

data class Database(@Deprecated("Going to be removed") val enabled: Boolean = true, val database: String = "")
