package no.fintlabs.operator.application

import com.sksamuel.hoplite.PropertySource
import io.fabric8.kubernetes.api.model.*
import io.fabric8.kubernetes.api.model.apps.Deployment
import no.fintlabs.extensions.KubernetesOperatorContext
import no.fintlabs.extensions.KubernetesResources
import no.fintlabs.loadConfig
import no.fintlabs.operator.application.Utils.createAndGetResource
import no.fintlabs.operator.application.Utils.createKoinTestExtension
import no.fintlabs.operator.application.Utils.createKubernetesOperatorExtension
import no.fintlabs.operator.application.Utils.createTestFlaisApplication
import no.fintlabs.operator.application.api.*
import no.fintlabs.v1alpha1.kafkauserandaclspec.Acls
import org.junit.jupiter.api.extension.RegisterExtension
import org.koin.dsl.module
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

@KubernetesResources("deployment/kubernetes")
class DeploymentDRTest {
    //region General
    @Test
    fun `should create deployment`(context: KubernetesOperatorContext) {
        val flaisApplication = createTestFlaisApplication()

        val deployment = context.createAndGetDeployment(flaisApplication)
        assertNotNull(deployment)
        assertEquals("test", deployment.metadata.name)

        assertEquals("test", deployment.metadata.labels["app"])
        assertEquals("test.org", deployment.metadata.labels["fintlabs.no/org-id"])
        assertEquals("test", deployment.metadata.labels["fintlabs.no/team"])

        assert(deployment.spec.selector.matchLabels.containsKey("app"))
        assertEquals("test", deployment.spec.selector.matchLabels["app"])

        assertEquals(1, deployment.spec.replicas)

        assertEquals(2, deployment.spec.template.spec.imagePullSecrets.size)
        assertEquals("reg-key-1", deployment.spec.template.spec.imagePullSecrets[0].name)
        assertEquals("reg-key-2", deployment.spec.template.spec.imagePullSecrets[1].name)


        assertEquals(1, deployment.spec.template.spec.containers.size)

        assertEquals("test-image", deployment.spec.template.spec.containers[0].image)
        assertEquals("test", deployment.spec.template.spec.containers[0].name)
        assertEquals("Always", deployment.spec.template.spec.containers[0].imagePullPolicy)

        assertEquals(1, deployment.spec.template.spec.containers[0].ports.size)
        assertEquals("http", deployment.spec.template.spec.containers[0].ports[0].name)
        assertEquals(8080, deployment.spec.template.spec.containers[0].ports[0].containerPort)

        assertEquals(2, deployment.spec.template.spec.containers[0].env.size)
    }

    //endregion

    //region Image
    @Test
    fun `should create deployment with correct container pull policy`(context: KubernetesOperatorContext) {
        val flaisApplication = createTestFlaisApplication().apply {
            spec = spec.copy(image = "test-image:latest")
        }

        val deployment = context.createAndGetDeployment(flaisApplication)
        assertNotNull(deployment)
        assertEquals("Always", deployment.spec.template.spec.containers[0].imagePullPolicy)
    }

    @Test
    fun `should create deployment with correct image pull secrets`(context: KubernetesOperatorContext) {
        context.create(Secret().apply {
            metadata = ObjectMeta().apply {
                name = "test-secret"
                type = "kubernetes.io/dockerconfigjson"
            }
            stringData = mapOf(
                ".dockerconfigjson" to "{}"
            )
        })

        val flaisApplication = createTestFlaisApplication().apply {
            spec = spec.copy(
                imagePullSecrets = listOf(
                    "test-secret"
                )
            )
        }

        val deployment = context.createAndGetDeployment(flaisApplication)
        assertNotNull(deployment)
        assertEquals(3, deployment.spec.template.spec.imagePullSecrets.size)
        assertEquals("test-secret", deployment.spec.template.spec.imagePullSecrets[2].name)
    }

    //endregion

    //region Resources
    @Test
    fun `should have correct resource limits`(context: KubernetesOperatorContext) {
        val flaisApplication = createTestFlaisApplication().apply {
            spec = spec.copy(
                resources = ResourceRequirementsBuilder()
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
        assertEquals("500m", deployment.spec.template.spec.containers[0].resources.requests["cpu"]?.toString())
        assertEquals("512Mi", deployment.spec.template.spec.containers[0].resources.requests["memory"]?.toString())
        assertEquals(2, deployment.spec.template.spec.containers[0].resources.limits.size)
        assertEquals("1", deployment.spec.template.spec.containers[0].resources.limits["cpu"]?.toString())
        assertEquals("1Gi", deployment.spec.template.spec.containers[0].resources.limits["memory"]?.toString())
    }
    //endregion

    //region Secrets and Environment variables
    @Test
    fun `should have additional env variables`(context: KubernetesOperatorContext) {
        val flaisApplication = createTestFlaisApplication().apply {
            spec = spec.copy(env = listOf(
                EnvVar().apply {
                    name = "key1"
                    value = "value1"
                },
                EnvVar().apply {
                    name = "key2"
                    value = "value2"
                }

            ))
        }

        val deployment = context.createAndGetDeployment(flaisApplication)
        assertNotNull(deployment)
        assertEquals(4, deployment.spec.template.spec.containers[0].env.size)
        assertEquals("fint.org-id", deployment.spec.template.spec.containers[0].env[0].name)
        assertEquals("test.org", deployment.spec.template.spec.containers[0].env[0].value)
        assertEquals("TZ", deployment.spec.template.spec.containers[0].env[1].name)
        assertEquals("Europe/Oslo", deployment.spec.template.spec.containers[0].env[1].value)
        assertEquals("key1", deployment.spec.template.spec.containers[0].env[2].name)
        assertEquals("value1", deployment.spec.template.spec.containers[0].env[2].value)
        assertEquals("key2", deployment.spec.template.spec.containers[0].env[3].name)
        assertEquals("value2", deployment.spec.template.spec.containers[0].env[3].value)
    }

    @Test
    fun `should have additional envFrom variable from 1Password`(context: KubernetesOperatorContext) {
        val flaisApplication = createTestFlaisApplication().apply {
            spec = spec.copy(
                onePassword = OnePassword(
                    itemPath = "test"
                )
            )
        }

        val deployment = context.createAndGetDeployment(flaisApplication)
        assertNotNull(deployment)
        assertEquals(1, deployment.spec.template.spec.containers[0].envFrom.size)
        assertEquals(
            "${flaisApplication.metadata.name}-op",
            deployment.spec.template.spec.containers[0].envFrom[0].secretRef.name
        )
    }

    @Test
    fun `should have additional envFrom variable from database`(context: KubernetesOperatorContext) {
        val flaisApplication = createTestFlaisApplication().apply {
            spec = spec.copy(
                database = Database("test-db")
            )
        }

        val deployment = context.createAndGetDeployment(flaisApplication)
        assertNotNull(deployment)
        assertEquals(1, deployment.spec.template.spec.containers[0].envFrom.size)
        assertEquals(
            "${flaisApplication.metadata.name}-db",
            deployment.spec.template.spec.containers[0].envFrom[0].secretRef.name
        )
    }

    @Test
    fun `should have additional envFrom variable from Kafka`(context: KubernetesOperatorContext) {
        val flaisApplication = createTestFlaisApplication().apply {
            spec = spec.copy(
                kafka = Kafka(
                    acls = listOf(
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
            deployment.spec.template.spec.containers[0].envFrom[0].secretRef.name
        )
    }

    @Test
    fun `should have correct path env vars`(context: KubernetesOperatorContext) {
        val flaisApplication = createTestFlaisApplication().apply {
            spec = spec.copy(
                url = Url(
                    basePath = "/test"
                )
            )
        }

        val deployment = context.createAndGetDeployment(flaisApplication)
        assertNotNull(deployment)
        assertEquals(4, deployment.spec.template.spec.containers[0].env.size)
        assertEquals("spring.webflux.base-path", deployment.spec.template.spec.containers[0].env[2].name)
        assertEquals("/test", deployment.spec.template.spec.containers[0].env[2].value)
        assertEquals("spring.mvc.servlet.path", deployment.spec.template.spec.containers[0].env[3].name)
        assertEquals("/test", deployment.spec.template.spec.containers[0].env[3].value)
    }
    //endregion

    //region Volumes and volume mounts
    @Test
    fun `should have volume mounts for Kafka`(context: KubernetesOperatorContext) {
        val flaisApplication = createTestFlaisApplication().apply {
            spec = spec.copy(
                kafka = Kafka(
                    acls = listOf(
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
        assertEquals("/credentials", deployment.spec.template.spec.containers[0].volumeMounts[0].mountPath)
        assertEquals(true, deployment.spec.template.spec.containers[0].volumeMounts[0].readOnly)
    }
    //endregion


    private fun KubernetesOperatorContext.createAndGetDeployment(app: FlaisApplicationCrd) =
        createAndGetResource<Deployment>(app)

    companion object {
        @RegisterExtension
        val koinTestExtension = createKoinTestExtension(module {
            single {
                loadConfig(PropertySource.resource("/deployment/application.yaml", optional = false))
            }
        })

        @RegisterExtension
        val kubernetesOperatorExtension = createKubernetesOperatorExtension()
    }
}