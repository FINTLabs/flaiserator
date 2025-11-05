package no.fintlabs.common.api.v1alpha1

data class FlaisResourceStatus(
    val observedGeneration: Long? = null,
    val state: FlaisResourceState = FlaisResourceState.PENDING,
    val correlationId: String? = null,
    val errors: List<StatusError>? = null,
)
