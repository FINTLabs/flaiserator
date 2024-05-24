package no.fintlabs.operator.application

import io.javaoperatorsdk.operator.api.reconciler.*
import io.javaoperatorsdk.operator.api.reconciler.dependent.Dependent
import no.fintlabs.operator.application.api.*
import java.util.*
import kotlin.jvm.optionals.getOrDefault

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
class FlaisApplicationReconciler : Reconciler<FlaisApplicationCrd>, Cleaner<FlaisApplicationCrd>, ContextInitializer<FlaisApplicationCrd> {
    override fun reconcile(resource: FlaisApplicationCrd, context: Context<FlaisApplicationCrd>): UpdateControl<FlaisApplicationCrd> {
        val workflowResult = context.managedDependentResourceContext().workflowReconcileResult.get()
        val ready = workflowResult.allDependentResourcesReady()
        val failed = workflowResult.erroredDependentsExist()

        val updateResource = correlationIdGenerated(context)

        return when {
            failed -> updateStateIfNeeded(resource, FlaisApplicationState.FAILED, updateResource)
            ready -> updateStateIfNeeded(resource, FlaisApplicationState.DEPLOYED, updateResource)
            else -> updateStateIfNeeded(resource, FlaisApplicationState.PENDING, updateResource)
        }
    }

    private fun updateStateIfNeeded(resource: FlaisApplicationCrd, newState: FlaisApplicationState, updateResource: Boolean): UpdateControl<FlaisApplicationCrd> = when {
        resource.status.state != newState && updateResource -> UpdateControl.updateResourceAndStatus(resource)
        resource.status.state != newState -> UpdateControl.updateStatus(resource.apply { status = status.copy(state = newState) })
        updateResource -> UpdateControl.updateResource(resource)
        else -> UpdateControl.noUpdate()
    }

    override fun cleanup(resource: FlaisApplicationCrd, context: Context<FlaisApplicationCrd>): DeleteControl {
        return DeleteControl.defaultDelete()
    }

    override fun initContext(primary: FlaisApplicationCrd, context: Context<FlaisApplicationCrd>) {
        ensureCorrelationId(primary, context)
    }

    private fun ensureCorrelationId(primary: FlaisApplicationCrd, context: Context<FlaisApplicationCrd>) {
        if(primary.metadata.annotations[DEPLOYMENT_CORRELATION_ID_ANNOTATION] == null) {
            context.managedDependentResourceContext().put(DEPLOYMENT_CORRELATION_ID_GENERATED, true)
            primary.metadata.annotations[DEPLOYMENT_CORRELATION_ID_ANNOTATION] = UUID.randomUUID().toString()
        }
    }

    private fun correlationIdGenerated(context: Context<FlaisApplicationCrd>) =
      context.managedDependentResourceContext().get(DEPLOYMENT_CORRELATION_ID_GENERATED, Boolean::class.javaObjectType).getOrDefault(false)

    companion object {
        const val DEPLOYMENT_CORRELATION_ID_GENERATED = "deployment-correlation-id-generated"
    }
}