package no.fintlabs.operator.application.api

import io.javaoperatorsdk.operator.api.ObservedGenerationAwareStatus

class FlaisApplicationStatus : ObservedGenerationAwareStatus() {
    var state: FlaisApplicationState = FlaisApplicationState.PENDING
    var dependentResourceStatus: List<String> = emptyList()
}