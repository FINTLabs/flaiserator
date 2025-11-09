package no.fintlabs.job

import io.fabric8.kubernetes.api.model.ObjectMeta
import io.fabric8.kubernetes.api.model.batch.v1.CronJob
import io.fabric8.kubernetes.api.model.batch.v1.Job
import io.javaoperatorsdk.operator.api.reconciler.Context
import io.javaoperatorsdk.operator.processing.dependent.Creator
import io.javaoperatorsdk.operator.processing.dependent.kubernetes.KubernetesDependent
import io.javaoperatorsdk.operator.processing.dependent.kubernetes.KubernetesDependentResource
import no.fintlabs.common.createOwnerReference
import no.fintlabs.common.getRequiredSecondaryResource
import no.fintlabs.job.api.v1alpha1.FlaisJob
import no.fintlabs.operator.dependent.ReadyCondition
import no.fintlabs.operator.dependent.ReconcileCondition
import org.koin.core.component.KoinComponent
import kotlin.jvm.optionals.getOrNull

private const val maxNameLen = 48

@KubernetesDependent
class JobDR :
  KubernetesDependentResource<Job, FlaisJob>(Job::class.java),
  KoinComponent, Creator<Job, FlaisJob>, ReconcileCondition<FlaisJob>, ReadyCondition<FlaisJob> {

  override fun name(): String = "job"

  override fun desired(primary: FlaisJob, context: Context<FlaisJob>): Job {
    val cronJob = context.getRequiredSecondaryResource<CronJob>()

    return Job().apply {
      metadata = ObjectMeta().apply {
        name = "${cronJob.metadata.name.take(maxNameLen)}-${cronJob.metadata.generation}"
        namespace = primary.metadata.namespace
        annotations = mutableMapOf(
          "cronjob.kubernetes.io/instantiate" to "manual"
        )
        ownerReferences = mutableListOf(createOwnerReference(cronJob).apply {
          controller = true
          blockOwnerDeletion = true
        })
      }
      spec = cronJob.spec.jobTemplate.spec
    }
  }

  override fun shouldReconcile(
    primary: FlaisJob,
    context: Context<FlaisJob>
  ): Boolean = primary.spec.schedule.isNullOrEmpty()

  override fun isReady(
    primary: FlaisJob,
    context: Context<FlaisJob>
  ): Boolean {
    return getSecondaryResource(primary, context).getOrNull()?.let { job ->
      job.status.conditions.find { it.type == "Complete" || it.type == "Failed" } != null
    } ?: false
  }
}