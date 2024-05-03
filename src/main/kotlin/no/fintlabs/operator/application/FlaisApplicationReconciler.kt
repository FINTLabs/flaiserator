package no.fintlabs.operator.application

import io.javaoperatorsdk.operator.api.reconciler.*
import no.fintlabs.operator.application.api.FlaisApplicationCrd
import no.fintlabs.operator.application.api.FlaisApplicationState
import java.time.Duration

@ControllerConfiguration
class FlaisApplicationReconciler : Reconciler<FlaisApplicationCrd>, Cleaner<FlaisApplicationCrd> {
    override fun reconcile(resource: FlaisApplicationCrd, context: Context<FlaisApplicationCrd>): UpdateControl<FlaisApplicationCrd> {
        val workflowResult = context.managedDependentResourceContext().workflowReconcileResult.get()
        val ready = workflowResult.allDependentResourcesReady()
        val failed = workflowResult.erroredDependentsExist()

        return when {
            failed -> updateStateIfNeeded(resource, FlaisApplicationState.FAILED)
            ready -> updateStateIfNeeded(resource, FlaisApplicationState.DEPLOYED)
            else -> updateStateIfNeeded(resource, FlaisApplicationState.PENDING)
        }
    }

    private fun updateStateIfNeeded(resource: FlaisApplicationCrd, newState: FlaisApplicationState): UpdateControl<FlaisApplicationCrd> {
        if (resource.status.state == newState) {
            return UpdateControl.noUpdate()
        }
        return UpdateControl.updateStatus(resource.apply { status.state = newState }).apply { if (newState == FlaisApplicationState.PENDING) rescheduleAfter(Duration.ofSeconds(5)) }
    }

    override fun cleanup(resource: FlaisApplicationCrd, context: Context<FlaisApplicationCrd>): DeleteControl {
        return DeleteControl.defaultDelete()
    }
}