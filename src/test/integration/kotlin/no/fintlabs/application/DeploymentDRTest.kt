package no.fintlabs.application

import com.sksamuel.hoplite.PropertySource
import io.fabric8.kubernetes.api.model.*
import io.fabric8.kubernetes.api.model.apps.Deployment
import io.fabric8.kubernetes.api.model.apps.DeploymentStrategy
import io.fabric8.kubernetes.api.model.apps.RollingUpdateDeployment
import io.fabric8.kubernetes.client.KubernetesClientException
import io.github.netmikey.logunit.api.LogCapturer
import no.fintlabs.application.Utils.createAndGetResource
import no.fintlabs.application.Utils.createKoinTestExtension
import no.fintlabs.application.Utils.createKubernetesOperatorExtension
import no.fintlabs.application.Utils.createTestFlaisApplication
import no.fintlabs.application.Utils.updateAndGetResource
import no.fintlabs.application.Utils.waitUntil
import no.fintlabs.application.api.LOKI_LOGGING_LABEL
import no.fintlabs.application.api.v1alpha1.*
import no.fintlabs.application.api.v1alpha1.Probe
import no.fintlabs.common.api.v1alpha1.FlaisResourceState
import no.fintlabs.common.api.v1alpha1.Kafka
import no.fintlabs.common.api.v1alpha1.OnePassword
import no.fintlabs.extensions.KubernetesOperator
import no.fintlabs.extensions.KubernetesOperatorContext
import no.fintlabs.extensions.KubernetesResources
import no.fintlabs.loadConfig
import no.fintlabs.v1alpha1.kafkauserandaclspec.Acls
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.RegisterExtension
import org.koin.dsl.module
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

@KubernetesResources("deployment/kubernetes")
class DeploymentDRTest {
  // region General
  @Test
  fun `should create deployment`(context: KubernetesOperatorContext) {
    val flaisApplication = createTestFlaisApplication()

    val deployment = context.createAndGetDeployment(flaisApplication)
    assertNotNull(deployment)
    assertEquals("test", deployment.metadata.name)

    assertEquals("test", deployment.metadata.labels["app"])
    assertEquals("test.org", deployment.metadata.labels["fintlabs.no/org-id"])
    assertEquals("test", deployment.metadata.labels["fintlabs.no/team"])

    assertEquals(
        "test",
        deployment.spec.template.metadata.annotations["kubectl.kubernetes.io/default-container"],
    )

    assert(deployment.spec.selector.matchLabels.containsKey("app"))
    assertEquals("test", deployment.spec.selector.matchLabels["app"])

    assertEquals(1, deployment.spec.replicas)

    assertEquals(2, deployment.spec.template.spec.imagePullSecrets.size)
    assertEquals("reg-key-1", deployment.spec.template.spec.imagePullSecrets[0].name)
    assertEquals("reg-key-2", deployment.spec.template.spec.imagePullSecrets[1].name)

    assertEquals(1, deployment.spec.template.spec.containers.size)

    assertEquals("hello-world", deployment.spec.template.spec.containers[0].image)
    assertEquals("test", deployment.spec.template.spec.containers[0].name)
    assertEquals("Always", deployment.spec.template.spec.containers[0].imagePullPolicy)

    assertEquals(1, deployment.spec.template.spec.containers[0].ports.size)
    assertEquals("http", deployment.spec.template.spec.containers[0].ports[0].name)
    assertEquals(8080, deployment.spec.template.spec.containers[0].ports[0].containerPort)

    assertEquals(2, deployment.spec.template.spec.containers[0].env.size)
  }

  // endregion

  // region Deployment
  @Test
  fun `should create deployment with correct replicas`(context: KubernetesOperatorContext) {
    val flaisApplication = createTestFlaisApplication().apply { spec = spec.copy(replicas = 3) }

    val deployment = context.createAndGetDeployment(flaisApplication)
    assertNotNull(deployment)
    assertEquals(3, deployment.spec.replicas)
  }

  @Test
  fun `should throw error on invalid replicas`(context: KubernetesOperatorContext) {
    val flaisApplication = createTestFlaisApplication().apply { spec = spec.copy(replicas = -1) }

    val exception =
        assertThrows<KubernetesClientException> { context.createAndGetDeployment(flaisApplication) }
    assert("spec.replicas: Invalid value: -1" in exception.status.message)
  }

  @Test
  fun `should create deployment with rolling update strategy`(context: KubernetesOperatorContext) {
    val flaisApplication =
        createTestFlaisApplication().apply {
          spec =
              spec.copy(
                  strategy =
                      DeploymentStrategy().apply {
                        type = "RollingUpdate"
                        rollingUpdate =
                            RollingUpdateDeployment().apply {
                              maxSurge = IntOrString("25%")
                              maxUnavailable = IntOrString("25%")
                            }
                      }
              )
        }

    val deployment = context.createAndGetDeployment(flaisApplication)
    assertNotNull(deployment)
    assertEquals("RollingUpdate", deployment.spec.strategy.type)
    assertEquals("25%", deployment.spec.strategy.rollingUpdate.maxSurge.strVal)
    assertEquals("25%", deployment.spec.strategy.rollingUpdate.maxUnavailable.strVal)
  }

  @Test
  fun `should create deployment with recreate strategy`(context: KubernetesOperatorContext) {
    val flaisApplication =
        createTestFlaisApplication().apply {
          spec = spec.copy(strategy = DeploymentStrategy().apply { type = "Recreate" })
        }

    val deployment = context.createAndGetDeployment(flaisApplication)
    assertNotNull(deployment)
    assertEquals("Recreate", deployment.spec.strategy.type)
  }

  @Test
  fun `should update deployment with correct image`(context: KubernetesOperatorContext) {
    val flaisApplication =
        createTestFlaisApplication().apply { spec = spec.copy(image = "hello-world:latest") }

    var deployment = context.createAndGetDeployment(flaisApplication)
    assertNotNull(deployment)
    assertEquals("hello-world:latest", deployment.spec.template.spec.containers[0].image)

    flaisApplication.spec = flaisApplication.spec.copy(image = "hello-world:linux")
    deployment = context.updateAndGetResource(flaisApplication)
    assertNotNull(deployment)
    assertEquals("hello-world:linux", deployment.spec.template.spec.containers[0].image)
  }

  // endregion

  // region Metadata
  @Test
  fun `should create deployment with correct labels`(context: KubernetesOperatorContext) {
    val flaisApplication =
        createTestFlaisApplication().apply {
          metadata = metadata.apply { labels = labels.plus("test" to "test") }
        }

    val deployment = context.createAndGetDeployment(flaisApplication)
    assertNotNull(deployment)
    assertEquals("test", deployment.metadata.labels["test"])
  }

  @Test
  fun `should add managed by label`(context: KubernetesOperatorContext) {
    val flaisApplication = createTestFlaisApplication()

    val deployment = context.createAndGetDeployment(flaisApplication)
    assertNotNull(deployment)
    assertEquals("flaiserator", deployment.metadata.labels["app.kubernetes.io/managed-by"])
  }

  // endregion

  // region Image
  @Test
  fun `should create deployment with correct container pull policy`(
      context: KubernetesOperatorContext
  ) {
    val flaisApplication =
        createTestFlaisApplication().apply { spec = spec.copy(image = "hello-world:latest") }

    val deployment = context.createAndGetDeployment(flaisApplication)
    assertNotNull(deployment)
    assertEquals("Always", deployment.spec.template.spec.containers[0].imagePullPolicy)
  }

  @Test
  fun `should create deployment with correct image pull secrets`(
      context: KubernetesOperatorContext
  ) {
    context.create(
        Secret().apply {
          metadata =
              ObjectMeta().apply {
                name = "test-secret"
                type = "kubernetes.io/dockerconfigjson"
              }
          stringData = mapOf(".dockerconfigjson" to "{}")
        }
    )

    val flaisApplication =
        createTestFlaisApplication().apply {
          spec = spec.copy(imagePullSecrets = listOf("test-secret"))
        }

    val deployment = context.createAndGetDeployment(flaisApplication)
    assertNotNull(deployment)
    assertEquals(3, deployment.spec.template.spec.imagePullSecrets.size)
    assertEquals("test-secret", deployment.spec.template.spec.imagePullSecrets[0].name)
  }

  // endregion

  // region Resources
  @Test
  fun `should have correct resource limits`(context: KubernetesOperatorContext) {
    val flaisApplication =
        createTestFlaisApplication().apply {
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

    val deployment = context.createAndGetDeployment(flaisApplication)
    assertNotNull(deployment)
    assertEquals(2, deployment.spec.template.spec.containers[0].resources.requests.size)
    assertEquals(
        "500m",
        deployment.spec.template.spec.containers[0].resources.requests["cpu"]?.toString(),
    )
    assertEquals(
        "512Mi",
        deployment.spec.template.spec.containers[0].resources.requests["memory"]?.toString(),
    )
    assertEquals(2, deployment.spec.template.spec.containers[0].resources.limits.size)
    assertEquals(
        "1",
        deployment.spec.template.spec.containers[0].resources.limits["cpu"]?.toString(),
    )
    assertEquals(
        "1Gi",
        deployment.spec.template.spec.containers[0].resources.limits["memory"]?.toString(),
    )
  }

  // endregion

  // region Secrets and Environment variables
  @Test
  fun `should have additional env variables`(context: KubernetesOperatorContext) {
    val flaisApplication =
        createTestFlaisApplication().apply {
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

    val deployment = context.createAndGetDeployment(flaisApplication)
    assertNotNull(deployment)
    assertEquals(4, deployment.spec.template.spec.containers[0].env.size)
    assertEquals("key1", deployment.spec.template.spec.containers[0].env[0].name)
    assertEquals("value1", deployment.spec.template.spec.containers[0].env[0].value)
    assertEquals("key2", deployment.spec.template.spec.containers[0].env[1].name)
    assertEquals("value2", deployment.spec.template.spec.containers[0].env[1].value)
    assertEquals("fint.org-id", deployment.spec.template.spec.containers[0].env[2].name)
    assertEquals("test.org", deployment.spec.template.spec.containers[0].env[2].value)
    assertEquals("TZ", deployment.spec.template.spec.containers[0].env[3].name)
    assertEquals("Europe/Oslo", deployment.spec.template.spec.containers[0].env[3].value)
  }

  @Test
  fun `should not have overlapping env variables`(context: KubernetesOperatorContext) {
    val flaisApplication =
        createTestFlaisApplication().apply {
          spec =
              spec.copy(
                  env =
                      listOf(
                          EnvVar().apply {
                            name = "fint.org-id"
                            value = "value1"
                          },
                          EnvVar().apply {
                            name = "key2"
                            value = "value2"
                          },
                      )
              )
        }

    val deployment = context.createAndGetDeployment(flaisApplication)
    assertNotNull(deployment)
    assertEquals(3, deployment.spec.template.spec.containers[0].env.size)
    assertEquals("fint.org-id", deployment.spec.template.spec.containers[0].env[0].name)
    assertEquals("value1", deployment.spec.template.spec.containers[0].env[0].value)
    assertEquals("key2", deployment.spec.template.spec.containers[0].env[1].name)
    assertEquals("value2", deployment.spec.template.spec.containers[0].env[1].value)
    assertEquals("TZ", deployment.spec.template.spec.containers[0].env[2].name)
    assertEquals("Europe/Oslo", deployment.spec.template.spec.containers[0].env[2].value)
  }

  @Test
  fun `should nullify value in env with empty string value`(context: KubernetesOperatorContext) {
    val flaisApplication =
        createTestFlaisApplication().apply {
          spec =
              spec.copy(
                  env =
                      listOf(
                          EnvVar().apply {
                            name = "fint.org-id"
                            value = ""
                          }
                      )
              )
        }

    val deployment = context.createAndGetDeployment(flaisApplication)
    assertNotNull(deployment)
    assertEquals("fint.org-id", deployment.spec.template.spec.containers[0].env[0].name)
    assertEquals(null, deployment.spec.template.spec.containers[0].env[0].value)
  }

  @Test
  fun `should have additional envFrom variable from 1Password`(context: KubernetesOperatorContext) {
    val flaisApplication =
        createTestFlaisApplication().apply {
          spec = spec.copy(onePassword = OnePassword(itemPath = "test"))
        }

    val deployment = context.createAndGetDeployment(flaisApplication)
    assertNotNull(deployment)
    assertEquals(1, deployment.spec.template.spec.containers[0].envFrom.size)
    assertEquals(
        "${flaisApplication.metadata.name}-op",
        deployment.spec.template.spec.containers[0].envFrom[0].secretRef.name,
    )
  }

  @Test
  fun `should have additional envFrom variable from database`(context: KubernetesOperatorContext) {
    val flaisApplication =
        createTestFlaisApplication().apply { spec = spec.copy(database = Database("test-db")) }

    val deployment = context.createAndGetDeployment(flaisApplication)
    assertNotNull(deployment)
    assertEquals(1, deployment.spec.template.spec.containers[0].envFrom.size)
    assertEquals(
        "${flaisApplication.metadata.name}-db",
        deployment.spec.template.spec.containers[0].envFrom[0].secretRef.name,
    )
  }

  @Test
  fun `should have additional envFrom variable from Kafka`(context: KubernetesOperatorContext) {
    val flaisApplication =
        createTestFlaisApplication().apply {
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

    val deployment = context.createAndGetDeployment(flaisApplication)
    assertNotNull(deployment)
    assertEquals(1, deployment.spec.template.spec.containers[0].envFrom.size)
    assertEquals(
        "${flaisApplication.metadata.name}-kafka",
        deployment.spec.template.spec.containers[0].envFrom[0].secretRef.name,
    )
  }

  @Test
  fun `should have correct path env vars`(context: KubernetesOperatorContext) {
    val flaisApplication =
        createTestFlaisApplication().apply { spec = spec.copy(url = Url(basePath = "/test")) }

    val deployment = context.createAndGetDeployment(flaisApplication)
    assertNotNull(deployment)
    assertEquals(4, deployment.spec.template.spec.containers[0].env.size)
    assertEquals(
        "spring.webflux.base-path",
        deployment.spec.template.spec.containers[0].env[2].name,
    )
    assertEquals("/test", deployment.spec.template.spec.containers[0].env[2].value)
    assertEquals("spring.mvc.servlet.path", deployment.spec.template.spec.containers[0].env[3].name)
    assertEquals("/test", deployment.spec.template.spec.containers[0].env[3].value)
  }

  // endregion

  // region Volumes and volume mounts
  @Test
  fun `should have volume for Kafka`(context: KubernetesOperatorContext) {
    val flaisApplication =
        createTestFlaisApplication().apply {
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

    val deployment = context.createAndGetDeployment(flaisApplication)
    assertNotNull(deployment)
    assertEquals(1, deployment.spec.template.spec.volumes.size)
    assertEquals("credentials", deployment.spec.template.spec.volumes[0].name)
    assertEquals(
        "test-kafka-certificates",
        deployment.spec.template.spec.volumes[0].secret.secretName,
    )
  }

  @Test
  fun `should have volume mounts for Kafka`(context: KubernetesOperatorContext) {
    val flaisApplication =
        createTestFlaisApplication().apply {
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

    val deployment = context.createAndGetDeployment(flaisApplication)
    assertNotNull(deployment)
    assertEquals(1, deployment.spec.template.spec.volumes.size)
    assertEquals("credentials", deployment.spec.template.spec.volumes[0].name)
    assertEquals(1, deployment.spec.template.spec.containers[0].volumeMounts.size)
    assertEquals("credentials", deployment.spec.template.spec.containers[0].volumeMounts[0].name)
    assertEquals(
        "/credentials",
        deployment.spec.template.spec.containers[0].volumeMounts[0].mountPath,
    )
    assertEquals(true, deployment.spec.template.spec.containers[0].volumeMounts[0].readOnly)
  }

  // endregion

  // region observability
  @Test
  fun `should have loki logging enabled by default`(context: KubernetesOperatorContext) {
    val flaisApplication = createTestFlaisApplication()

    val deployment = context.createAndGetDeployment(flaisApplication)
    assertNotNull(deployment)
    assertEquals("true", deployment.spec.template.metadata.labels[LOKI_LOGGING_LABEL])
  }

  @Test
  fun `should have loki logging enabled`(context: KubernetesOperatorContext) {
    val flaisApplication =
        createTestFlaisApplication().apply {
          spec = spec.copy(observability = Observability(logging = Logging(loki = true)))
        }

    val deployment = context.createAndGetDeployment(flaisApplication)
    assertNotNull(deployment)
    assertEquals("true", deployment.spec.template.metadata.labels[LOKI_LOGGING_LABEL])
  }

  @Test
  fun `should have loki logging disabled`(context: KubernetesOperatorContext) {
    val flaisApplication =
        createTestFlaisApplication().apply {
          spec = spec.copy(observability = Observability(logging = Logging(loki = false)))
        }

    val deployment = context.createAndGetDeployment(flaisApplication)
    assertNotNull(deployment)
    assertEquals("false", deployment.spec.template.metadata.labels[LOKI_LOGGING_LABEL])
  }

  @Test
  fun `should have metric port open when metrics are enabled and app and metric port differ`(
      context: KubernetesOperatorContext
  ) {
    val flaisApplication =
        createTestFlaisApplication().apply {
          spec =
              spec.copy(
                  observability =
                      Observability(
                          metrics = Metrics(enabled = true, port = "8081", path = "/metrics")
                      )
              )
        }

    val deployment = context.createAndGetDeployment(flaisApplication)
    assertNotNull(deployment)
    assertEquals(2, deployment.spec.template.spec.containers[0].ports.size)
    assertEquals("http", deployment.spec.template.spec.containers[0].ports[0].name)
    assertEquals(8080, deployment.spec.template.spec.containers[0].ports[0].containerPort)
    assertEquals("metrics", deployment.spec.template.spec.containers[0].ports[1].name)
    assertEquals(8081, deployment.spec.template.spec.containers[0].ports[1].containerPort)
  }

  // endregion

  // region PodSelector

  @Test
  @KubernetesResources("deployment/custom-pod-selector.yaml")
  @KubernetesOperator(explicitStart = true)
  fun `should recreate deployment on pod selector change selector`(
      context: KubernetesOperatorContext
  ) {
    val application = assertNotNull(context.get<FlaisApplication>("test"))
    var deployment = assertNotNull(context.get<Deployment>("test"))

    deployment.metadata.ownerReferences.add(createOwnerReference(application))

    context.update(deployment)
    context.operator.start()

    context.waitUntil<FlaisApplication>(application.metadata.name) {
      it.status?.state == FlaisResourceState.DEPLOYED
    }

    deployment = assertNotNull(context.get<Deployment>(application.metadata.name))
    assertEquals(1, deployment.spec.selector.matchLabels.size)
    assert(deployment.spec.selector.matchLabels.containsKey("app"))
    assertEquals(deployment.metadata.name, deployment.spec.selector.matchLabels["app"])
  }

  @Test
  fun `should not recreate deployment on pod selector match`(context: KubernetesOperatorContext) {
    val flaisApplication = createTestFlaisApplication()

    var deployment = context.createAndGetDeployment(flaisApplication)
    assertNotNull(deployment)

    flaisApplication.apply { spec = spec.copy(image = "hello-world:linux") }

    deployment = context.updateAndGetResource(flaisApplication)
    assertNotNull(deployment)
    logs.assertDoesNotContain("Pod selector does not match, recreating deployment")
  }

  // endregion

  // region Probes
  @Test
  fun `should create default probes`(context: KubernetesOperatorContext) {
    val flaisApplication =
        createTestFlaisApplication().apply {
          spec =
              spec.copy(probes = Probes(startup = Probe(), liveness = Probe(), readiness = Probe()))
        }
    val deployment = context.createAndGetDeployment(flaisApplication)
    assertNotNull(deployment)
    val appContainer =
        deployment.spec.template.spec.containers.find { it.name == flaisApplication.metadata.name }
    assertNotNull(appContainer)

    val startupProbe = appContainer.startupProbe
    assertNotNull(startupProbe)
    assertEquals("/", startupProbe.httpGet.path)
    assertEquals(flaisApplication.spec.port, startupProbe.httpGet.port.intVal)
    assertEquals(ProbeDefaults.PERIOD_SECONDS, startupProbe.periodSeconds)
    assertEquals(ProbeDefaults.TIMEOUT_SECONDS, startupProbe.timeoutSeconds)
    assertEquals(ProbeDefaults.FAILURE_THRESHOLD, startupProbe.failureThreshold)
    assertNull(startupProbe.initialDelaySeconds)

    val livenessProbe = appContainer.startupProbe
    assertNotNull(livenessProbe)
    assertEquals("/", livenessProbe.httpGet.path)
    assertEquals(flaisApplication.spec.port, livenessProbe.httpGet.port.intVal)
    assertEquals(ProbeDefaults.PERIOD_SECONDS, livenessProbe.periodSeconds)
    assertEquals(ProbeDefaults.TIMEOUT_SECONDS, livenessProbe.timeoutSeconds)
    assertEquals(ProbeDefaults.FAILURE_THRESHOLD, livenessProbe.failureThreshold)
    assertNull(livenessProbe.initialDelaySeconds)

    val readinessProbe = appContainer.startupProbe
    assertNotNull(readinessProbe)
    assertEquals("/", readinessProbe.httpGet.path)
    assertEquals(flaisApplication.spec.port, readinessProbe.httpGet.port.intVal)
    assertEquals(ProbeDefaults.PERIOD_SECONDS, readinessProbe.periodSeconds)
    assertEquals(ProbeDefaults.TIMEOUT_SECONDS, readinessProbe.timeoutSeconds)
    assertEquals(ProbeDefaults.FAILURE_THRESHOLD, readinessProbe.failureThreshold)
    assertNull(readinessProbe.initialDelaySeconds)
  }

  @Test
  fun `should be able to set custom probe parameters`(context: KubernetesOperatorContext) {
    val flaisApplication =
        createTestFlaisApplication().apply {
          spec =
              spec.copy(
                  probes =
                      Probes(
                          liveness =
                              Probe(
                                  path = "/liveness",
                                  port = IntOrString(8080),
                                  periodSeconds = 100,
                                  timeoutSeconds = 100,
                                  failureThreshold = 100,
                                  initialDelaySeconds = 100,
                              )
                      )
              )
        }

    val deployment = context.createAndGetDeployment(flaisApplication)
    assertNotNull(deployment)
    val appContainer =
        deployment.spec.template.spec.containers.find { it.name == flaisApplication.metadata.name }
    assertNotNull(appContainer)

    val livenessProbe = appContainer.livenessProbe
    assertNotNull(livenessProbe)

    assertEquals("/liveness", livenessProbe.httpGet.path)
    assertEquals(8080, livenessProbe.httpGet.port.intVal)
    assertEquals(100, livenessProbe.periodSeconds)
    assertEquals(100, livenessProbe.timeoutSeconds)
    assertEquals(100, livenessProbe.failureThreshold)
    assertEquals(100, livenessProbe.initialDelaySeconds)
  }

  @Test
  fun `should add leading slash if missing`(context: KubernetesOperatorContext) {
    val flaisApplication =
        createTestFlaisApplication().apply {
          spec = spec.copy(probes = Probes(startup = Probe(path = "some/path")))
        }
    val deployment = context.createAndGetDeployment(flaisApplication)
    assertNotNull(deployment)
    val appContainer =
        deployment.spec.template.spec.containers.find { it.name == flaisApplication.metadata.name }
    assertNotNull(appContainer)

    val startupProbe = appContainer.startupProbe
    assertNotNull(startupProbe)

    assertEquals("/some/path", startupProbe.httpGet.path)
  }

  @Test
  fun `should use kubernetes defaults if probe values are 0`(context: KubernetesOperatorContext) {
    val flaisApplication =
        createTestFlaisApplication().apply {
          spec =
              spec.copy(
                  probes =
                      Probes(
                          liveness =
                              Probe(
                                  initialDelaySeconds = 0,
                                  failureThreshold = 0,
                                  periodSeconds = 0,
                                  timeoutSeconds = 0,
                              )
                      )
              )
        }

    val deployment = context.createAndGetDeployment(flaisApplication)
    assertNotNull(deployment)
    val appContainer =
        deployment.spec.template.spec.containers.find { it.name == flaisApplication.metadata.name }
    assertNotNull(appContainer)
    val livenessProbe = appContainer.livenessProbe
    assertNotNull(livenessProbe)
    assertEquals(ProbeDefaults.PERIOD_SECONDS, livenessProbe.periodSeconds)
    assertEquals(ProbeDefaults.TIMEOUT_SECONDS, livenessProbe.timeoutSeconds)
    assertEquals(ProbeDefaults.FAILURE_THRESHOLD, livenessProbe.failureThreshold)
    assertNull(livenessProbe.initialDelaySeconds)
  }

  @Test
  fun `should use kubernetes defaults if probe values are null`(
      context: KubernetesOperatorContext
  ) {
    val flaisApplication =
        createTestFlaisApplication().apply {
          spec =
              spec.copy(
                  probes =
                      Probes(
                          liveness =
                              Probe(
                                  initialDelaySeconds = null,
                                  failureThreshold = null,
                                  periodSeconds = null,
                                  timeoutSeconds = null,
                              )
                      )
              )
        }

    val deployment = context.createAndGetDeployment(flaisApplication)
    assertNotNull(deployment)
    val appContainer =
        deployment.spec.template.spec.containers.find { it.name == flaisApplication.metadata.name }
    assertNotNull(appContainer)
    val livenessProbe = appContainer.livenessProbe
    assertNotNull(livenessProbe)
    assertEquals(ProbeDefaults.PERIOD_SECONDS, livenessProbe.periodSeconds)
    assertEquals(ProbeDefaults.TIMEOUT_SECONDS, livenessProbe.timeoutSeconds)
    assertEquals(ProbeDefaults.FAILURE_THRESHOLD, livenessProbe.failureThreshold)
    assertNull(livenessProbe.initialDelaySeconds)
  }

  // endregion

  private fun KubernetesOperatorContext.createAndGetDeployment(app: FlaisApplication) =
      createAndGetResource<Deployment>(app)

  @RegisterExtension
  val logs: LogCapturer = LogCapturer.create().captureForType(DeploymentDR::class.java)

  companion object {
    @RegisterExtension
    val koinTestExtension =
        createKoinTestExtension(
            module {
              single {
                loadConfig(
                    PropertySource.resource("/deployment/application.yaml", optional = false)
                )
              }
            }
        )

    @RegisterExtension val kubernetesOperatorExtension = createKubernetesOperatorExtension()
  }
}
