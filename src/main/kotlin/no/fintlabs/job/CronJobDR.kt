package no.fintlabs.job

import io.fabric8.kubernetes.api.model.Container
import io.fabric8.kubernetes.api.model.batch.v1.CronJob
import io.fabric8.kubernetes.api.model.batch.v1.CronJobSpec
import io.fabric8.kubernetes.api.model.batch.v1.JobSpec
import io.fabric8.kubernetes.api.model.batch.v1.JobTemplateSpec
import io.javaoperatorsdk.operator.api.config.informer.Informer
import io.javaoperatorsdk.operator.api.reconciler.Context
import io.javaoperatorsdk.operator.processing.dependent.kubernetes.CRUDKubernetesDependentResource
import io.javaoperatorsdk.operator.processing.dependent.kubernetes.KubernetesDependent
import no.fintlabs.Config
import no.fintlabs.application.api.MANAGED_BY_FLAISERATOR_SELECTOR
import no.fintlabs.common.KafkaDR
import no.fintlabs.common.OnePasswordDR
import no.fintlabs.common.PostgresUserDR
import no.fintlabs.common.createObjectMeta
import no.fintlabs.common.getLogger
import no.fintlabs.common.pod.PodBuilder
import no.fintlabs.common.pod.PodBuilderContext
import no.fintlabs.job.api.v1alpha1.FlaisJob
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

@KubernetesDependent(informer = Informer(labelSelector = MANAGED_BY_FLAISERATOR_SELECTOR))
class CronJobDR :
  CRUDKubernetesDependentResource<CronJob, FlaisJob>(CronJob::class.java),
  KoinComponent {
  private val config: Config by inject()
  private val logger = getLogger()

  private val kafkaDR by inject<KafkaDR<FlaisJob>>()
  private val postgresUserDR by inject<PostgresUserDR<FlaisJob>>()
  private val onePasswordDR by inject<OnePasswordDR<FlaisJob>>()
  private val podBuilder = PodBuilder.create(
    config,
    kafkaDR,
    postgresUserDR,
    onePasswordDR
  )

  override fun name() = "cronJob"

  override fun desired(primary: FlaisJob, context: Context<FlaisJob>): CronJob {
    val podTemplate = podBuilder.build(
      primary,
      context,
      { builderContext -> createPodMetadata(primary, builderContext) },
      { builderContext -> configurePodSpec(primary, builderContext) }
    )

    var schedule = primary.spec.schedule
    var suspend = false
    if (schedule.isNullOrEmpty()) {
      schedule = "0 0 1 1 *"
      suspend = true
    }

    return CronJob().apply {
      metadata = createObjectMeta(primary)
      spec = CronJobSpec().apply {
        timeZone = primary.spec.timezone
        this.schedule = schedule
        this.suspend = suspend
        successfulJobsHistoryLimit = primary.spec.successfulJobsHistoryLimit
        failedJobsHistoryLimit = primary.spec.failedJobsHistoryLimit
        concurrencyPolicy = primary.spec.concurrencyPolicy
        jobTemplate = JobTemplateSpec().apply {
          metadata = createObjectMeta(primary)
          spec = JobSpec().apply {
            activeDeadlineSeconds = primary.spec.activeDeadlineSeconds
            backoffLimit = primary.spec.backoffLimit
            ttlSecondsAfterFinished = primary.spec.ttlSecondsAfterFinished
            template = podTemplate.apply {
              spec.restartPolicy = primary.spec.restartPolicy
            }
          }
        }
      }
    }
  }

  fun createPodMetadata(primary: FlaisJob, builderContext: PodBuilderContext) =
    createObjectMeta(primary).apply {
      annotations.putAll(builderContext.annotations)
      labels.putAll(builderContext.labels)

      annotations["kubectl.kubernetes.io/default-container"] = primary.metadata.name
      labels["observability.fintlabs.no/loki"] =
        primary.spec.observability?.logging?.loki?.toString() ?: "true"
    }

  fun configurePodSpec(primary: FlaisJob, builderContext: PodBuilderContext) {
    builderContext.envFrom.addAll(primary.spec.envFrom)
    builderContext.containers += Container().apply {
      name = primary.metadata.name
      image = primary.spec.image
      imagePullPolicy = primary.spec.imagePullPolicy
      resources = primary.spec.resources
      env = builderContext.env
      envFrom = builderContext.envFrom
      volumeMounts = builderContext.volumeMounts
    }
  }
}