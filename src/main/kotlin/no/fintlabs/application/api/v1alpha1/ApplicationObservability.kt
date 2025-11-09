package no.fintlabs.application.api.v1alpha1

import no.fintlabs.common.api.v1alpha1.Observability

data class ApplicationObservability(
    val metrics: Metrics? = null,
    override val logging: Logging? = null,
) : Observability
