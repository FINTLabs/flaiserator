package no.fintlabs.operator.application.api

class FlaisApplicationStatus {
    var dependentResourceStatus: List<String> = emptyList()
    var observedGeneration: Int = 0
}