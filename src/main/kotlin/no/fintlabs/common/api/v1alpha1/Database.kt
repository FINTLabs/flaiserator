package no.fintlabs.common.api.v1alpha1

data class Database(val database: String? = null) {
  @Deprecated("Going to be removed") val enabled: Boolean = true
}