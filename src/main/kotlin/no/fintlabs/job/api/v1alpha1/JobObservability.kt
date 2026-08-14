package no.fintlabs.job.api.v1alpha1

import no.fintlabs.common.api.v1alpha1.Observability

data class JobObservability(
    val logging: JobLogging?,
) : Observability

data class JobLogging(
    val loki: Boolean? = null,
)
