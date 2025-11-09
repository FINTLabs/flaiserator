package no.fintlabs.job

import io.fabric8.kubernetes.api.model.batch.v1.CronJob
import io.fabric8.kubernetes.api.model.batch.v1.Job
import no.fintlabs.Utils.updateAndGetResource
import no.fintlabs.extensions.KubernetesOperatorContext
import no.fintlabs.job.Utils.createAndGetResource
import no.fintlabs.job.Utils.createJobKoinTestExtension
import no.fintlabs.job.Utils.createJobKubernetesOperatorExtension
import no.fintlabs.job.Utils.createTestFlaisJob
import no.fintlabs.job.api.v1alpha1.FlaisJob
import org.junit.jupiter.api.extension.RegisterExtension
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.junit.jupiter.params.provider.ValueSource
import org.junit.runners.Parameterized
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull

class JobDRTest {
  @Test
  fun `should create Job`(context: KubernetesOperatorContext) {
    val flaisJob = createTestFlaisJob().apply {
      spec = spec.copy(schedule = null)
    }

    val job = context.createAndGetJob(flaisJob)
    assertNotNull(job)
  }

  @Test
  fun `update should create new job`(context: KubernetesOperatorContext) {
    val flaisJob = createTestFlaisJob().apply {
      spec = spec.copy(schedule = null)
    }

    val firstJob = context.createAndGetJob(flaisJob)
    assertNotNull(firstJob)

    val secondJob = context.updateAndGetJob(flaisJob.apply {
      spec = spec.copy(timezone = "Europe/Stockholm")
    })
    assertNotNull(secondJob)

    assertNotEquals(firstJob.metadata.name, secondJob.metadata.name)
    val jobs = context.kubernetesClient.resources(Job::class.java)
      .inNamespace(context.namespace)
      .resources()
      .filter { it.get().metadata.name.startsWith(flaisJob.metadata.name) }
      .toList()
    assertEquals(2, jobs.size)
  }

  @Test
  fun `job should have owner reference to cron job`(context: KubernetesOperatorContext) {
    val flaisJob = createTestFlaisJob().apply {
      spec = spec.copy(schedule = null)
    }

    val job = context.createAndGetJob(flaisJob)
    assertNotNull(job)
    val cronJob = context.get<CronJob>(flaisJob.metadata.name)
    assertNotNull(cronJob)

    val jobOwnerRev = job.metadata.ownerReferences.first { it.kind == "CronJob" }
    assertNotNull(jobOwnerRev)
    assertEquals(cronJob.metadata.uid, jobOwnerRev.uid)
    assert(jobOwnerRev.blockOwnerDeletion)
    assert(jobOwnerRev.controller)
  }

  @ParameterizedTest
  @CsvSource(
    "hello-world, Complete, ",
    "nginx, Failed, 1",
  )
  fun `should wait for job result before reconciliation`(
    image: String, conditionType: String, deadlineSeconds: Long?,
    context: KubernetesOperatorContext
  ) {
    val flaisJob = createTestFlaisJob().apply {
      spec = spec.copy(schedule = null, activeDeadlineSeconds = deadlineSeconds, image = image)
    }

    val job = context.createAndGetJob(flaisJob)
    assertNotNull(job)
    val lastCondition = job.status.conditions.last()
    assertEquals( conditionType, lastCondition.type )
  }

  companion object {
    private fun KubernetesOperatorContext.createAndGetJob(job: FlaisJob) =
      createAndGetResource<Job>(job) {
        val cronJob = get<CronJob>(job.metadata.name)
        assertNotNull(cronJob)
        "${cronJob.metadata.name}-${cronJob.metadata.generation}"
      }

    private fun KubernetesOperatorContext.updateAndGetJob(job: FlaisJob) =
      updateAndGetResource<FlaisJob, Job>(job) {
        val cronJob = get<CronJob>(job.metadata.name)
        assertNotNull(cronJob)
        "${cronJob.metadata.name}-${cronJob.metadata.generation}"
      }

    @RegisterExtension
    val koinTestExtension = createJobKoinTestExtension()

    @RegisterExtension val kubernetesOperatorExtension = createJobKubernetesOperatorExtension()
  }
}
