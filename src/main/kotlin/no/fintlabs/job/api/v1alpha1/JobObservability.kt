package no.fintlabs.job.api.v1alpha1

import no.fintlabs.application.api.v1alpha1.Logging
import no.fintlabs.common.api.v1alpha1.Observability

data class JobObservability(override val logging: Logging?) : Observability
