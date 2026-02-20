package no.fintlabs.job

import io.fabric8.kubernetes.api.model.EnvVar
import io.fabric8.kubernetes.api.model.Quantity
import io.fabric8.kubernetes.api.model.ResourceRequirementsBuilder
import io.fabric8.kubernetes.api.model.batch.v1.CronJob
import io.javaoperatorsdk.operator.api.reconciler.Reconciler
import io.javaoperatorsdk.operator.processing.retry.GenericRetry
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import no.fintlabs.Utils.updateAndGetResource
import no.fintlabs.application.Utils.createTestFlaisApplication
import no.fintlabs.application.api.LOKI_LOGGING_LABEL
import no.fintlabs.application.api.v1alpha1.Logging
import no.fintlabs.common.api.v1alpha1.Database
import no.fintlabs.common.api.v1alpha1.Kafka
import no.fintlabs.common.api.v1alpha1.OnePassword
import no.fintlabs.extensions.KubernetesOperator
import no.fintlabs.extensions.KubernetesOperatorContext
import no.fintlabs.job.Utils.createAndGetResource
import no.fintlabs.job.Utils.createJobKoinTestExtension
import no.fintlabs.job.Utils.createJobKubernetesOperatorExtension
import no.fintlabs.job.Utils.createTestFlaisJob
import no.fintlabs.job.api.v1alpha1.FlaisJob
import no.fintlabs.job.api.v1alpha1.JobObservability
import no.fintlabs.v1alpha1.kafkauserandaclspec.Acls
import org.junit.jupiter.api.extension.RegisterExtension
import org.koin.core.qualifier.named
import org.koin.test.KoinTest
import org.koin.test.get
import kotlin.test.assertNull

class CronJobDRTest : KoinTest {
  @Test
  fun `should create cron job`(context: KubernetesOperatorContext) {
    val flaisJob = createTestFlaisJob()

    val cronJob = context.createAndGetCronJob(flaisJob)
    assertNotNull(cronJob)
    assertEquals("test", cronJob.metadata.name)

    assertEquals("test", cronJob.metadata.labels["app"])
    assertEquals("test.org", cronJob.metadata.labels["fintlabs.no/org-id"])
    assertEquals("test", cronJob.metadata.labels["fintlabs.no/team"])

    val cronJobSpec = cronJob.spec
    assertEquals(flaisJob.spec.successfulJobsHistoryLimit, cronJobSpec.successfulJobsHistoryLimit)
    assertEquals(flaisJob.spec.failedJobsHistoryLimit, cronJobSpec.failedJobsHistoryLimit)
    assertEquals(flaisJob.spec.concurrencyPolicy, cronJobSpec.concurrencyPolicy)

    val jobSpec = cronJobSpec.jobTemplate.spec
    assertEquals(flaisJob.spec.backoffLimit, jobSpec.backoffLimit)

    val podTemplate = jobSpec.template
    assertEquals(
        "test",
        podTemplate.metadata.annotations["kubectl.kubernetes.io/default-container"],
    )
    assertEquals("Never", podTemplate.spec.restartPolicy)

    assertEquals(1, podTemplate.spec.containers.size)

    val appContainer = podTemplate.spec.containers.first { it.name == flaisJob.metadata.name }
    assertNotNull(appContainer)
    assertEquals("hello-world", appContainer.image)
    assertEquals("test", appContainer.name)
    assertEquals("Always", appContainer.imagePullPolicy)
  }

  @Test
  fun `should suspend if no schedule`(context: KubernetesOperatorContext) {
    val flaisJob = createTestFlaisJob().apply { spec = spec.copy(schedule = null) }

    val cronJob = context.createAndGetCronJob(flaisJob)
    assertNotNull(cronJob)
    assert(cronJob.spec.suspend)
  }

  @Test
  @KubernetesOperator(explicitStart = true, registerReconcilers = false)
  fun `should throw error on invalid schedule`(context: KubernetesOperatorContext) {
    val reconciler = get<Reconciler<*>>(named("flais-job-reconciler")) as JobReconciler
    context.registerReconciler(reconciler) { it.withRetry(GenericRetry.noRetry()) }
    context.operator.start()

    val flaisJob = createTestFlaisJob().apply { spec = spec.copy(schedule = "this is not valid") }

    val actualFlaisJob = context.createAndGetResource<FlaisJob>(flaisJob)
    assertNotNull(actualFlaisJob)
    val errors = actualFlaisJob.status.errors
    assertNotNull(errors)
    assertEquals(1, errors.size)
    assertContains(errors[0].message, "Invalid value: \"this is not valid\"")
  }

  @Test
  fun `should update cron job with correct image`(context: KubernetesOperatorContext) {
    val flaisJob = createTestFlaisJob().apply { spec = spec.copy(image = "hello-world:latest") }

    var cronJob = context.createAndGetCronJob(flaisJob)
    assertNotNull(cronJob)
    var containers = cronJob.spec.jobTemplate.spec.template.spec.containers
    var appContainer = containers.first { it.name == flaisJob.metadata.name }
    assertEquals("hello-world:latest", appContainer.image)

    flaisJob.spec = flaisJob.spec.copy(image = "hello-world:linux")
    cronJob = context.updateAndGetResource(flaisJob)
    assertNotNull(cronJob)
    containers = cronJob.spec.jobTemplate.spec.template.spec.containers
    appContainer = containers.first { it.name == flaisJob.metadata.name }
    assertEquals("hello-world:linux", appContainer.image)
  }

  @Test
  fun `should add managed by label`(context: KubernetesOperatorContext) {
    val flaisJob = createTestFlaisJob()

    val cronJob = context.createAndGetCronJob(flaisJob)
    assertNotNull(cronJob)
    assertEquals("flaiserator", cronJob.metadata.labels["app.kubernetes.io/managed-by"])
  }

  @Test
  fun `should create deployment with correct restart policy`(context: KubernetesOperatorContext) {
    val flaisJob = createTestFlaisJob().apply { spec = spec.copy(restartPolicy = "OnFailure") }

    val cronJob = context.createAndGetCronJob(flaisJob)
    assertNotNull(cronJob)
    assertEquals("OnFailure", cronJob.spec.jobTemplate.spec.template.spec.restartPolicy)
  }

  @Test
  fun `should not set cpu resource limits`(context: KubernetesOperatorContext) {
    val flaisJob = createTestFlaisJob().apply {
      spec =
        spec.copy(
          resources = ResourceRequirementsBuilder().build()
        )
    }

    val cronJob = context.createAndGetCronJob(flaisJob)
    assertNotNull(cronJob)
    val containers = cronJob.spec.jobTemplate.spec.template.spec.containers
    val appContainer = containers.first { it.name == flaisJob.metadata.name }
    assertNull(appContainer.resources.limits["cpu"])
  }

  @Test
  fun `should have correct resource limits`(context: KubernetesOperatorContext) {
    val flaisJob =
        createTestFlaisJob().apply {
          spec =
              spec.copy(
                  resources =
                      ResourceRequirementsBuilder()
                          .addToRequests("cpu", Quantity("500m"))
                          .addToRequests("memory", Quantity("512Mi"))
                          .addToLimits("cpu", Quantity("1"))
                          .addToLimits("memory", Quantity("1Gi"))
                          .build()
              )
        }

    val cronJob = context.createAndGetCronJob(flaisJob)
    assertNotNull(cronJob)
    val containers = cronJob.spec.jobTemplate.spec.template.spec.containers
    val appContainer = containers.first { it.name == flaisJob.metadata.name }
    assertEquals(2, appContainer.resources.requests.size)
    assertEquals(
        "500m",
        appContainer.resources.requests["cpu"]?.toString(),
    )
    assertEquals(
        "512Mi",
        appContainer.resources.requests["memory"]?.toString(),
    )
    assertEquals(2, appContainer.resources.limits.size)
    assertEquals(
        "1",
        appContainer.resources.limits["cpu"]?.toString(),
    )
    assertEquals(
        "1Gi",
        appContainer.resources.limits["memory"]?.toString(),
    )
  }

  @Test
  fun `should have additional env variables`(context: KubernetesOperatorContext) {
    val flaisJob =
        createTestFlaisJob().apply {
          spec =
              spec.copy(
                  env =
                      listOf(
                          EnvVar().apply {
                            name = "key1"
                            value = "value1"
                          },
                          EnvVar().apply {
                            name = "key2"
                            value = "value2"
                          },
                      )
              )
        }

    val cronJob = context.createAndGetCronJob(flaisJob)
    assertNotNull(cronJob)
    val containers = cronJob.spec.jobTemplate.spec.template.spec.containers
    val appContainer = containers.first { it.name == flaisJob.metadata.name }
    val env = appContainer.env
    assert(env.size > 2)
    val key1 = env.find { it.name == "key1" }
    assertEquals("value1", key1!!.value)
    val key2 = env.find { it.name == "key2" }
    assertEquals("value2", key2!!.value)
  }

  @Test
  fun `should have additional envFrom variable from 1Password`(context: KubernetesOperatorContext) {
    val flaisJob =
        createTestFlaisJob().apply {
          spec = spec.copy(onePassword = OnePassword(itemPath = "test"))
        }

    val cronJob = context.createAndGetCronJob(flaisJob)
    assertNotNull(cronJob)
    val containers = cronJob.spec.jobTemplate.spec.template.spec.containers
    val appContainer = containers.first { it.name == flaisJob.metadata.name }
    assertTrue { appContainer.envFrom.any { it.secretRef.name == "${flaisJob.metadata.name}-op" } }
  }

  @Test
  fun `should have additional envFrom variable from database`(context: KubernetesOperatorContext) {
    val flaisJob = createTestFlaisJob().apply { spec = spec.copy(database = Database("test-db")) }

    val cronJob = context.createAndGetCronJob(flaisJob)
    assertNotNull(cronJob)
    val containers = cronJob.spec.jobTemplate.spec.template.spec.containers
    val appContainer = containers.first { it.name == flaisJob.metadata.name }
    assertTrue { appContainer.envFrom.any { it.secretRef.name == "${flaisJob.metadata.name}-db" } }
  }

  @Test
  fun `should have additional envFrom variable and volume mount from Kafka`(
      context: KubernetesOperatorContext
  ) {
    val flaisJob =
        createTestFlaisJob().apply {
          spec =
              spec.copy(
                  kafka =
                      Kafka(
                          acls =
                              listOf(
                                  Acls().apply {
                                    topic = "test-topic"
                                    permission = "write"
                                  }
                              )
                      )
              )
        }

    val cronJob = context.createAndGetCronJob(flaisJob)
    assertNotNull(cronJob)
    val podSpec = cronJob.spec.jobTemplate.spec.template.spec
    val kafkaCredentialVolume = podSpec.volumes.first { it.name == "credentials" }
    assertNotNull(kafkaCredentialVolume)
    assertEquals(
        "${flaisJob.metadata.name}-kafka-certificates",
        kafkaCredentialVolume.secret.secretName,
    )
    val appContainer = podSpec.containers.first { it.name == flaisJob.metadata.name }
    assertTrue {
      appContainer.envFrom.any { it.secretRef.name == "${flaisJob.metadata.name}-kafka" }
    }
    val kafkaCredentialVolumeMount = appContainer.volumeMounts.first { it.name == "credentials" }
    assertNotNull(kafkaCredentialVolume)
    assertEquals("/credentials", kafkaCredentialVolumeMount.mountPath)
    assertTrue(kafkaCredentialVolumeMount.readOnly)
  }

  @Test
  fun `should have loki logging enabled by default`(context: KubernetesOperatorContext) {
    val flaisJob = createTestFlaisJob()

    val cronJob = context.createAndGetCronJob(flaisJob)
    assertNotNull(cronJob)
    assertEquals("true", cronJob.spec.jobTemplate.spec.template.metadata.labels[LOKI_LOGGING_LABEL])
  }

  @Test
  fun `should have loki logging enabled`(context: KubernetesOperatorContext) {
    val flaisJob =
        createTestFlaisJob().apply {
          spec = spec.copy(observability = JobObservability(logging = Logging(loki = true)))
        }

    val deployment = context.createAndGetCronJob(flaisJob)
    assertNotNull(deployment)
    assertEquals(
        "true",
        deployment.spec.jobTemplate.spec.template.metadata.labels[LOKI_LOGGING_LABEL],
    )
  }

  @Test
  fun `should have loki logging disabled`(context: KubernetesOperatorContext) {
    val flaisJob =
        createTestFlaisJob().apply {
          spec = spec.copy(observability = JobObservability(logging = Logging(loki = false)))
        }

    val deployment = context.createAndGetCronJob(flaisJob)
    assertNotNull(deployment)
    assertEquals(
        "false",
        deployment.spec.jobTemplate.spec.template.metadata.labels[LOKI_LOGGING_LABEL],
    )
  }

  companion object {
    private fun KubernetesOperatorContext.createAndGetCronJob(job: FlaisJob) =
        createAndGetResource<CronJob>(job)

    @RegisterExtension val koinTestExtension = createJobKoinTestExtension()

    @RegisterExtension val kubernetesOperatorExtension = createJobKubernetesOperatorExtension()
  }
}
