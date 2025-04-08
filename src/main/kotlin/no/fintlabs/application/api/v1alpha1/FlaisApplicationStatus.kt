package no.fintlabs.application.api.v1alpha1

data class FlaisApplicationStatus(
    val observedGeneration: Long? = null,
    val state: FlaisApplicationState = FlaisApplicationState.PENDING,
    val correlationId: String? = null,
    val errors: List<StatusError>? = null
)