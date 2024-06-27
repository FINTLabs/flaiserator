package no.fintlabs.operator.application.api.v1alpha1

import io.javaoperatorsdk.operator.api.ObservedGenerationAwareStatus

data class FlaisApplicationStatus(
    val state: FlaisApplicationState = FlaisApplicationState.PENDING,
    val correlationId: String? = null,
    val dependentResourceStatus: List<String> = emptyList()
) : ObservedGenerationAwareStatus()