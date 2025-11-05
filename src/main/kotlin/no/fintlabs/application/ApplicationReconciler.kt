package no.fintlabs.application

import io.javaoperatorsdk.operator.api.reconciler.Cleaner
import io.javaoperatorsdk.operator.api.reconciler.Context
import io.javaoperatorsdk.operator.api.reconciler.ControllerConfiguration
import io.javaoperatorsdk.operator.api.reconciler.DeleteControl
import io.javaoperatorsdk.operator.api.reconciler.ErrorStatusUpdateControl
import io.javaoperatorsdk.operator.api.reconciler.Reconciler
import io.javaoperatorsdk.operator.api.reconciler.UpdateControl
import io.javaoperatorsdk.operator.processing.retry.GradualRetry
import java.time.Duration
import java.util.*
import kotlin.jvm.optionals.getOrNull
import no.fintlabs.application.api.DEPLOYMENT_CORRELATION_ID_ANNOTATION
import no.fintlabs.application.api.v1alpha1.FlaisApplication
import no.fintlabs.application.api.v1alpha1.clone
import no.fintlabs.common.KafkaDR
import no.fintlabs.common.OnePasswordDR
import no.fintlabs.common.api.v1alpha1.FlaisResourceState
import no.fintlabs.common.api.v1alpha1.FlaisResourceStatus
import no.fintlabs.common.api.v1alpha1.StatusError
import no.fintlabs.operator.workflow.Dependent
import no.fintlabs.operator.workflow.Workflow
import org.koin.core.component.KoinComponent
import org.slf4j.MDC

@GradualRetry(maxAttempts = 3)
@ControllerConfiguration
@Workflow(
    [
        Dependent(DeploymentDR::class),
        Dependent(ServiceDR::class),
        Dependent(PodMetricsDR::class),
        Dependent(IngressDR::class),
        Dependent(PostgresUserDR::class),
        Dependent(OnePasswordDR::class),
        Dependent(KafkaDR::class),
    ]
)
class ApplicationReconciler :
    Reconciler<FlaisApplication>, Cleaner<FlaisApplication>, KoinComponent {
  private val logger = getLogger()

  override fun reconcile(
    resource: FlaisApplication,
    context: Context<FlaisApplication>,
  ): UpdateControl<FlaisApplication> {
    if (context.isNextReconciliationImminent) {
      return UpdateControl.noUpdate()
    }

    setMDC(resource)
    initReconciliation(resource, context)?.let {
      removeMDC()
      return it
    }

    context.managedWorkflowAndDependentResourceContext().reconcileManagedWorkflow()

    if (context.isNextReconciliationImminent) {
      removeMDC()
      return UpdateControl.noUpdate()
    }

    val resourceUpdate = resource.clone().apply { status = determineNewStatus(resource, context) }

    return UpdateControl.patchStatus(resourceUpdate).also { removeMDC() }
  }

  override fun cleanup(
      resource: FlaisApplication,
      context: Context<FlaisApplication>,
  ): DeleteControl {
    return DeleteControl.defaultDelete()
  }

  override fun updateErrorStatus(
      resource: FlaisApplication,
      context: Context<FlaisApplication>,
      e: Exception?,
  ): ErrorStatusUpdateControl<FlaisApplication> {
    setMDC(resource)
    val resourceUpdate = resource.clone().apply { status = determineNewStatus(resource, context) }

    return ErrorStatusUpdateControl.patchStatus(resourceUpdate).also { removeMDC() }
  }

  private fun initReconciliation(
      origResource: FlaisApplication,
      context: Context<FlaisApplication>,
  ): UpdateControl<FlaisApplication>? {
    val currentCorrelationId =
        origResource.metadata.annotations[DEPLOYMENT_CORRELATION_ID_ANNOTATION]
    val observedCorrelationId = origResource.status?.correlationId
    val observedGen = origResource.status?.observedGeneration
    val currentGen = origResource.metadata.generation

    val resourceUpdated = currentGen != observedGen

    val (correlationId, correlationIdUpdated) =
        if (
            currentCorrelationId.isNullOrBlank() ||
                (resourceUpdated && currentCorrelationId == observedCorrelationId)
        ) {
          UUID.randomUUID().toString() to true
        } else currentCorrelationId to false

    val updateStatus = resourceUpdated || correlationIdUpdated

    val patchResource =
        origResource.clone().apply {
          if (correlationIdUpdated) {
            metadata.annotations = mapOf(DEPLOYMENT_CORRELATION_ID_ANNOTATION to correlationId)
          }

          if (updateStatus) {
            status =
              FlaisResourceStatus(
                    observedGeneration = origResource.metadata.generation,
                    state = FlaisResourceState.PENDING,
                    correlationId = correlationId,
                )
          }
        }

    return when {
      correlationIdUpdated -> UpdateControl.patchResourceAndStatus(patchResource)
      updateStatus -> UpdateControl.patchStatus(patchResource)
      else -> null
    }?.apply { rescheduleAfter(Duration.ofMillis(100)) }
  }

  private fun determineNewStatus(
      resource: FlaisApplication,
      context: Context<FlaisApplication>,
  ): FlaisResourceStatus {
    val workflowResult =
        context.managedWorkflowAndDependentResourceContext().workflowReconcileResult.get()
    val ready = workflowResult.allDependentResourcesReady()
    val failed = workflowResult.erroredDependentsExist()
    val isLastAttempt = context.retryInfo.getOrNull()?.isLastAttempt ?: false

    for (dep in workflowResult.reconciledDependents) {
      logger.info("Reconciled dependent ${dep.name()}")
    }
    for (dep in workflowResult.notReadyDependents) {
      logger.info("Reconcile not ready for ${dep.name()}")
    }
    for ((dep, error) in workflowResult.erroredDependents) {
      logger.info("Reconcile error for ${dep.name()} - $error")
    }

    return resource.status.copy(
        state =
            when {
              isLastAttempt && failed -> FlaisResourceState.FAILED
              ready && !failed -> FlaisResourceState.DEPLOYED
              else -> FlaisResourceState.PENDING
            },
        errors =
            workflowResult.erroredDependents
                .map { StatusError(it.value.message ?: "", it.key.name()) }
                .takeIf { it.isNotEmpty() },
    )
  }

  private fun setMDC(resource: FlaisApplication) {
    MDC.put(
        "correlationId",
        resource.metadata.annotations[DEPLOYMENT_CORRELATION_ID_ANNOTATION] ?: "",
    )
  }

  private fun removeMDC() {
    MDC.remove("correlationId")
  }

  companion object {
    const val WORKFLOW_RESULT_KEY = "workflow_result"
  }
}
