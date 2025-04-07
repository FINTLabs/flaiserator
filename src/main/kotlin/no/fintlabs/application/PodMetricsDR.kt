package no.fintlabs.application

import com.coreos.monitoring.v1.PodMonitor
import com.coreos.monitoring.v1.PodMonitorSpec
import com.coreos.monitoring.v1.podmonitorspec.PodMetricsEndpoints
import com.coreos.monitoring.v1.podmonitorspec.Selector
import io.javaoperatorsdk.operator.api.config.informer.Informer
import io.javaoperatorsdk.operator.api.reconciler.Context
import io.javaoperatorsdk.operator.processing.dependent.kubernetes.CRUDKubernetesDependentResource
import io.javaoperatorsdk.operator.processing.dependent.kubernetes.KubernetesDependent
import no.fintlabs.application.api.MANAGED_BY_FLAISERATOR_SELECTOR
import no.fintlabs.application.api.v1alpha1.FlaisApplicationCrd
import no.fintlabs.operator.dependent.ReconcileCondition

@KubernetesDependent(
    informer = Informer(labelSelector = MANAGED_BY_FLAISERATOR_SELECTOR)
)
class PodMetricsDR : CRUDKubernetesDependentResource<PodMonitor, FlaisApplicationCrd>(PodMonitor::class.java), ReconcileCondition<FlaisApplicationCrd> {
    override fun name(): String = "pod-metrics"

    override fun desired(primary: FlaisApplicationCrd, context: Context<FlaisApplicationCrd>): PodMonitor = PodMonitor().apply {
        val metrics = primary.spec.observability?.metrics ?: primary.spec.prometheus
        val portName = if (metrics.port.toInt() == primary.spec.port) "http" else "metrics"

        metadata = createObjectMeta(primary)
        spec = PodMonitorSpec().apply {
            jobLabel = "app.kubernetes.io/name"
            podTargetLabels = listOf("app", "fintlabs.no/team", "fintlabs.no/org-id")
            podMetricsEndpoints = listOf(PodMetricsEndpoints().apply {
                port = portName
                path = metrics.path
                honorLabels = false
            })
            selector = Selector().apply {
                matchLabels = mapOf("app" to primary.metadata.name)
            }
        }
    }

    override fun shouldReconcile(primary: FlaisApplicationCrd, context: Context<FlaisApplicationCrd>): Boolean {
        val metrics = primary.spec.observability?.metrics ?: primary.spec.prometheus
        return metrics.enabled
    }
}
