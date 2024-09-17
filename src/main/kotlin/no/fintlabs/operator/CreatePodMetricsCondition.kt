package no.fintlabs.operator

import com.coreos.monitoring.v1.PodMonitor
import io.javaoperatorsdk.operator.api.reconciler.Context
import io.javaoperatorsdk.operator.api.reconciler.dependent.DependentResource
import io.javaoperatorsdk.operator.processing.dependent.workflow.Condition
import no.fintlabs.operator.api.v1alpha1.FlaisApplicationCrd

class CreatePodMetricsCondition : Condition<PodMonitor, FlaisApplicationCrd> {
    override fun isMet(
        dependentResource: DependentResource<PodMonitor, FlaisApplicationCrd>?,
        primary: FlaisApplicationCrd,
        context: Context<FlaisApplicationCrd>
    ) : Boolean {
        val metrics = primary.spec.observability?.metrics ?: primary.spec.prometheus
        return metrics.enabled
    }

}