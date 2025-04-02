package no.fintlabs.operator

import io.javaoperatorsdk.operator.api.config.informer.Informer
import io.javaoperatorsdk.operator.api.reconciler.*
import io.javaoperatorsdk.operator.processing.dependent.workflow.Workflow
import io.javaoperatorsdk.operator.processing.dependent.workflow.WorkflowBuilder
import io.javaoperatorsdk.operator.processing.dependent.workflow.WorkflowReconcileResult
import io.javaoperatorsdk.operator.processing.event.source.EventSource
import io.javaoperatorsdk.operator.processing.retry.GradualRetry
import no.fintlabs.operator.api.DEPLOYMENT_CORRELATION_ID_ANNOTATION
import no.fintlabs.operator.api.MANAGED_BY_FLAISERATOR_SELECTOR
import no.fintlabs.operator.api.ORG_ID_LABEL
import no.fintlabs.operator.api.TEAM_LABEL
import no.fintlabs.operator.api.v1alpha1.FlaisApplicationCrd
import no.fintlabs.operator.api.v1alpha1.FlaisApplicationState
import no.fintlabs.operator.api.v1alpha1.FlaisApplicationStatus
import org.koin.core.component.KoinComponent
import org.koin.core.component.get
import org.koin.core.component.inject
import org.slf4j.MDC
import java.util.*
import kotlin.jvm.optionals.getOrNull


@GradualRetry(maxAttempts = 3)
@ControllerConfiguration(
    informer = Informer(labelSelector = MANAGED_BY_FLAISERATOR_SELECTOR)
)
class FlaisApplicationReconciler : Reconciler<FlaisApplicationCrd>, Cleaner<FlaisApplicationCrd>, ErrorStatusHandler<FlaisApplicationCrd>, EventSourceInitializer<FlaisApplicationCrd>, KoinComponent {
    private val logger = getLogger()

    private val deployment by inject<DeploymentDR>()
    private val service by inject<ServiceDR>()
    private val podMetrics by inject<PodMetricsDR>()
    private val onePassword by inject<OnePasswordDR>()
    private val ingress by inject<IngressDR>()
    private val postgresUser by inject<PostgresUserDR>()
    private val kafka by inject<KafkaDR>()

    private val workflow: Workflow<FlaisApplicationCrd> = WorkflowBuilder<FlaisApplicationCrd>()
        .addDependentResource(deployment, DeploymentDR.COMPONENT)
        .addDependentResource(service, ServiceDR.COMPONENT)
        .addDependentResource(podMetrics, PodMetricsDR.COMPONENT)
            .withReconcilePrecondition(get<CreatePodMetricsCondition>())
        .addDependentResource(onePassword, OnePasswordDR.COMPONENT)
            .withReconcilePrecondition(get<CreateOnePasswordCondition>())
        .addDependentResource(ingress, IngressDR.COMPONENT)
            .withReconcilePrecondition(get<CreateIngressCondition>())
        .addDependentResource(postgresUser, PostgresUserDR.COMPONENT)
            .withReconcilePrecondition(get<CreatePostgresUserCondition>())
        .addDependentResource(kafka, KafkaDR.COMPONENT)
            .withReconcilePrecondition(get<CreateKafkaCondition>())
        .withThrowExceptionFurther(false)
        .build()

    override fun reconcile(resource: FlaisApplicationCrd, context: Context<FlaisApplicationCrd>): UpdateControl<FlaisApplicationCrd> {
        setMDC(resource)
        initReconciliation(resource)?.let {
            removeMDC()
            return it
        }

        val result = workflow.reconcile(resource, context)
        if (result.erroredDependentsExist()) {
            context.managedDependentResourceContext().put(WORKFLOW_RESULT_KEY, result)
            result.throwAggregateExceptionIfErrorsPresent()
        }
        val statusUpdated = updateStatus(resource, determineNewStatus(resource, context, result))

        return when {
            statusUpdated -> UpdateControl.updateStatus(resource)
            else -> UpdateControl.noUpdate()
        }.also {
            removeMDC()
        }
    }

    override fun cleanup(resource: FlaisApplicationCrd, context: Context<FlaisApplicationCrd>): DeleteControl {
        return DeleteControl.defaultDelete()
    }

    override fun updateErrorStatus(resource: FlaisApplicationCrd, context: Context<FlaisApplicationCrd>, e: Exception?): ErrorStatusUpdateControl<FlaisApplicationCrd> {
        val result = context.managedDependentResourceContext().get(WORKFLOW_RESULT_KEY, WorkflowReconcileResult::class.java)
        return when (updateStatus(resource, determineNewStatus(resource, context, result.getOrNull()))) {
            true -> ErrorStatusUpdateControl.updateStatus(resource)
            else -> ErrorStatusUpdateControl.noStatusUpdate()
        }
    }

    override fun prepareEventSources(context: EventSourceContext<FlaisApplicationCrd>): MutableMap<String, EventSource> =
        EventSourceInitializer.eventSourcesFromWorkflow(context, workflow)

    private fun initReconciliation(resource: FlaisApplicationCrd) : UpdateControl<FlaisApplicationCrd>? {
        val annotations = resource.metadata.annotations
        val hasCorrelationId = annotations.containsKey(DEPLOYMENT_CORRELATION_ID_ANNOTATION)
        if (!hasCorrelationId) {
            val uuid = UUID.randomUUID().toString()
            logger.debug("Generating correlation ID $uuid for ${resource.metadata.name}")
            annotations[DEPLOYMENT_CORRELATION_ID_ANNOTATION] = uuid
        }

        val newStatus = resource.status.copy(
            state = FlaisApplicationState.PENDING,
            correlationId = annotations[DEPLOYMENT_CORRELATION_ID_ANNOTATION]
        )
        val statusUpdated = updateStatus(resource, newStatus)

        return when {
            !hasCorrelationId -> UpdateControl.updateResourceAndStatus(resource)
            statusUpdated -> UpdateControl.updateStatus(resource)
            else -> null
        }?.rescheduleAfter(0)
    }

    private fun determineNewStatus(primary: FlaisApplicationCrd, context: Context<FlaisApplicationCrd>, workflowResult: WorkflowReconcileResult?): FlaisApplicationStatus {
        val ready = workflowResult?.allDependentResourcesReady() ?: false
        val failed = workflowResult?.erroredDependentsExist() ?: true
        val isLastAttempt = context.retryInfo.getOrNull()?.isLastAttempt ?: false

        if (workflowResult != null) {
            for ((dep, res) in workflowResult.reconcileResults) {
                logger.info("Reconcile result for ${dep.javaClass.simpleName} - ${res.singleOperation}")
            }
            for ((dep, error) in workflowResult.erroredDependents) {
                logger.info("Reconcile error for ${dep.javaClass.simpleName} - $error")
            }
        }

        return primary.status.copy(
            state = when {
                isLastAttempt && failed -> FlaisApplicationState.FAILED
                ready && !failed -> FlaisApplicationState.DEPLOYED
                else -> FlaisApplicationState.PENDING
            },
            dependentErrors = workflowResult?.erroredDependents?.map { it.key.resourceType().simpleName to (it.value.message ?: "") }?.toMap(),
            correlationId = primary.metadata.annotations[DEPLOYMENT_CORRELATION_ID_ANNOTATION]
        )
    }

    private fun updateStatus(primary: FlaisApplicationCrd, newStatus: FlaisApplicationStatus): Boolean {
        return if (primary.status != newStatus) {
            primary.status = newStatus
            true
        } else false
    }

    private fun setMDC(resource: FlaisApplicationCrd) {
        MDC.put("correlationId", resource.metadata.annotations[DEPLOYMENT_CORRELATION_ID_ANNOTATION] ?: "")
    }

    private fun removeMDC() {
        MDC.remove("correlationId")
    }

    companion object {
        const val WORKFLOW_RESULT_KEY = "workflow_result"
    }
}