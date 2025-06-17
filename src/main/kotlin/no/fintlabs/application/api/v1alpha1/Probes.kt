package no.fintlabs.application.api.v1alpha1

import io.fabric8.kubernetes.api.model.IntOrString

object ProbeDefaults {
    const val FAILURE_THRESHOLD = 3
    const val TIMEOUT_SECONDS = 1
    const val PERIOD_SECONDS = 10
}

data class Probes(
    val startup: Probe? = null,
    val readiness: Probe? = null,
    val liveness: Probe? = null,
)

data class Probe(
    val path: String? = null,
    val port: IntOrString? = null,
    val initialDelaySeconds: Int? = null,
    val failureThreshold: Int? = null,
    val periodSeconds: Int? = null,
    val timeoutSeconds: Int? = null,
)