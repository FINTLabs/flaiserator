package no.fintlabs.job

import io.javaoperatorsdk.operator.api.reconciler.ControllerConfiguration
import io.javaoperatorsdk.operator.processing.retry.GradualRetry
import no.fintlabs.application.DeploymentDR
import no.fintlabs.application.IngressDR
import no.fintlabs.application.PodMetricsDR
import no.fintlabs.application.ServiceDR
import no.fintlabs.common.FlaisResourceReconciler
import no.fintlabs.common.KafkaDR
import no.fintlabs.common.OnePasswordDR
import no.fintlabs.common.PostgresUserDR
import no.fintlabs.job.api.v1alpha1.FlaisJob
import no.fintlabs.operator.workflow.Dependent
import no.fintlabs.operator.workflow.DependentRef
import no.fintlabs.operator.workflow.Workflow

@GradualRetry(maxAttempts = 3)
@ControllerConfiguration
@Workflow(
  [
    Dependent(CronJobDR::class),
    Dependent(
      JobDR::class,
      dependsOn = [DependentRef(CronJobDR::class)],
    ),
    Dependent(PostgresUserDR::class),
    Dependent(OnePasswordDR::class),
    Dependent(KafkaDR::class),
  ]
)
class JobReconciler : FlaisResourceReconciler<FlaisJob>()
