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
    labelSelector = "$ORG_ID_LABEL,$TEAM_LABEL"
)
class FlaisApplicationReconciler : Reconciler<FlaisApplicationCrd>, Cleaner<FlaisApplicationCrd>,
    ContextInitializer<FlaisApplicationCrd> {
    override fun reconcile(
        resource: FlaisApplicationCrd,
        context: Context<FlaisApplicationCrd>
    ): UpdateControl<FlaisApplicationCrd> {
        return determineUpdateControl(resource, updateStatus(resource, context), updateResource(context))
    }

    private fun determineUpdateControl(
        resource: FlaisApplicationCrd,
        statusUpdated: Boolean,
        resourceUpdated: Boolean
    ) = when {
        statusUpdated && resourceUpdated -> UpdateControl.updateResourceAndStatus(resource)
        resourceUpdated -> UpdateControl.updateResource(resource)
        statusUpdated -> UpdateControl.updateStatus(resource)
        else -> UpdateControl.noUpdate()
    }

    override fun cleanup(resource: FlaisApplicationCrd, context: Context<FlaisApplicationCrd>): DeleteControl {
        return DeleteControl.defaultDelete()
    }

    override fun initContext(primary: FlaisApplicationCrd, context: Context<FlaisApplicationCrd>) {
        ensureCorrelationId(primary, context)
    }

    private fun updateResource(context: Context<FlaisApplicationCrd>): Boolean {
        return context.managedDependentResourceContext()
            .get(DEPLOYMENT_CORRELATION_ID_GENERATED, Boolean::class.javaObjectType)
            .getOrDefault(false)
    }

    private fun updateStatus(primary: FlaisApplicationCrd, context: Context<FlaisApplicationCrd>): Boolean {
        val newStatus = determineNewStatus(primary, context)
        return if (primary.status != newStatus) {
            primary.status = newStatus
            true
        } else false
    }

    private fun determineNewStatus(
        primary: FlaisApplicationCrd,
        context: Context<FlaisApplicationCrd>
    ): FlaisApplicationStatus {
        val workflowResult = context.managedDependentResourceContext().workflowReconcileResult.get()
        val ready = workflowResult.allDependentResourcesReady()
        val failed = workflowResult.erroredDependentsExist()

        return primary.status.copy(
            state = when {
                failed -> FlaisApplicationState.FAILED
                ready -> FlaisApplicationState.DEPLOYED
                else -> FlaisApplicationState.PENDING
            },
            correlationId = primary.metadata.annotations[DEPLOYMENT_CORRELATION_ID_ANNOTATION]
        )
    }

    private fun ensureCorrelationId(primary: FlaisApplicationCrd, context: Context<FlaisApplicationCrd>) {
        primary.metadata.annotations.computeIfAbsent(DEPLOYMENT_CORRELATION_ID_ANNOTATION) {
            context.managedDependentResourceContext().put(DEPLOYMENT_CORRELATION_ID_GENERATED, true)
            UUID.randomUUID().toString()
        }
    }

    companion object {
        const val DEPLOYMENT_CORRELATION_ID_GENERATED = "deployment-correlation-id-generated"
    }
}