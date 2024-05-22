package no.fintlabs.operator.application

import io.javaoperatorsdk.operator.api.reconciler.*
import io.javaoperatorsdk.operator.api.reconciler.dependent.Dependent
import no.fintlabs.operator.application.api.FlaisApplicationCrd
import no.fintlabs.operator.application.api.FlaisApplicationState

@ControllerConfiguration(
    dependents = [
        Dependent(
            name = DeploymentDR.COMPONENT,
            type = DeploymentDR::class
        ),
        Dependent(
            name = ServiceDR.COMPONENT,
            type = ServiceDR::class
        ),
        Dependent(
            name = OnePasswordDR.COMPONENT,
            type = OnePasswordDR::class,
            reconcilePrecondition = CreateOnePasswordCondition::class
        ),
        Dependent(
            name = IngressDR.COMPONENT,
            type = IngressDR::class,
            reconcilePrecondition = CreateIngressCondition::class
        ),
        Dependent(
            name = PostgresUserDR.COMPONENT,
            type = PostgresUserDR::class,
            reconcilePrecondition = CreatePostgresUserCondition::class
        ),
        Dependent(
            name = KafkaDR.COMPONENT,
            type = KafkaDR::class,
            reconcilePrecondition = CreateKafkaCondition::class
        )
    ],
    labelSelector = "fintlabs.no/team,fintlabs.no/org-id"
)
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
        return UpdateControl.updateStatus(resource.apply { status.state = newState })
    }

    override fun cleanup(resource: FlaisApplicationCrd, context: Context<FlaisApplicationCrd>): DeleteControl {
        return DeleteControl.defaultDelete()
    }
}