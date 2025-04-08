package no.fintlabs.application.api.v1alpha1

data class StatusError (
    val message: String,
    val dependent: String? = null
)