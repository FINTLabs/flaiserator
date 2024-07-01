package no.fintlabs.operator.application.api.v1alpha1

data class Logging(
    val loki: Boolean? = null,
    val secureLogs: Boolean? = null
)
