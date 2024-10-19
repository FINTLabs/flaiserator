package no.fintlabs.operator.api.v1alpha1

import io.javaoperatorsdk.operator.api.ObservedGenerationAwareStatus

data class FlaisApplicationStatus(
    val state: FlaisApplicationState = FlaisApplicationState.PENDING,
    val correlationId: String? = null,
    val dependentErrors: Map<String, String>? = null
) : ObservedGenerationAwareStatus()