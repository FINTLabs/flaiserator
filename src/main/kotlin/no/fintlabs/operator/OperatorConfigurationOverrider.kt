package no.fintlabs.operator

import io.fabric8.kubernetes.client.KubernetesClient
import io.javaoperatorsdk.operator.api.monitoring.Metrics

interface OperatorConfigurationOverrider {
    fun setMetrics(metrics: Metrics)
    fun setKubernetesClient(client: KubernetesClient)
    fun setCloseClientOnStop(stopClientOnStop: Boolean)
}