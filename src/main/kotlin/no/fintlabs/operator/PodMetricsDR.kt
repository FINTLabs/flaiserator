package no.fintlabs.operator

import com.coreos.monitoring.v1.PodMonitor
import com.coreos.monitoring.v1.PodMonitorSpec
import com.coreos.monitoring.v1.podmonitorspec.PodMetricsEndpoints
import com.coreos.monitoring.v1.podmonitorspec.Selector
import io.javaoperatorsdk.operator.api.reconciler.Context
import io.javaoperatorsdk.operator.processing.dependent.kubernetes.CRUDKubernetesDependentResource
import io.javaoperatorsdk.operator.processing.dependent.kubernetes.KubernetesDependent
import no.fintlabs.operator.api.MANAGED_BY_FLAISERATOR_SELECTOR
import no.fintlabs.operator.api.v1alpha1.FlaisApplicationCrd

@KubernetesDependent(
    labelSelector = MANAGED_BY_FLAISERATOR_SELECTOR
)
class PodMetricsDR : CRUDKubernetesDependentResource<PodMonitor, FlaisApplicationCrd>(PodMonitor::class.java) {
    override fun desired(primary: FlaisApplicationCrd, context: Context<FlaisApplicationCrd>): PodMonitor = PodMonitor().apply {
        val metrics = primary.spec.observability?.metrics ?: primary.spec.prometheus

        metadata = createObjectMeta(primary)
        spec = PodMonitorSpec().apply {
            jobLabel = "app.kubernetes.io/name"
            podTargetLabels = listOf("app", "fintlabs.no/team", "fintlabs.no/org-id")
            podMetricsEndpoints = listOf(PodMetricsEndpoints().apply {
                port = metrics.port
                path = metrics.path
                honorLabels = false
            })
            selector = Selector().apply {
                matchLabels = mapOf("app" to primary.metadata.name)
            }
        }
    }

    companion object {
        const val COMPONENT = "pod-metrics"
    }
}
