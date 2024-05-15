package no.fintlabs.operator.application

import io.fabric8.kubernetes.api.model.EnvVar
import io.fabric8.kubernetes.api.model.ObjectMeta
import io.fabric8.kubernetes.api.model.Quantity
import io.fabric8.kubernetes.api.model.ResourceRequirementsBuilder
import io.fabric8.kubernetes.api.model.Secret
import io.fabric8.kubernetes.api.model.SecretBuilder
import io.fabric8.kubernetes.api.model.apps.Deployment
import no.fintlabs.baseModule
import no.fintlabs.operator.KubernetesOperatorContext
import no.fintlabs.operator.KubernetesOperatorExtension
import no.fintlabs.operator.application.api.*
import org.awaitility.kotlin.atMost
import org.awaitility.kotlin.await
import org.awaitility.kotlin.until
import org.junit.jupiter.api.extension.RegisterExtension
import org.koin.test.junit5.KoinTestExtension
import java.time.Duration
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class DeploymentDependentResourceTest {
    @JvmField
    @RegisterExtension
    val koinTestExtension = KoinTestExtension.create {
        modules(baseModule, applicationModule())
    }

    @JvmField
    @RegisterExtension
    val extension = KubernetesOperatorExtension.create()

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
                    ImagePullSecret(name = "test-secret")
                )
            )
        }

        val deployment = context.createAndGetDeployment(flaisApplication)
        assertNotNull(deployment)
        assertEquals(1, deployment.spec.template.spec.imagePullSecrets.size)
        assertEquals("test-secret", deployment.spec.template.spec.imagePullSecrets[0].name)
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
                database = Database(
                    enabled = true,
                    database = "test-db"
                )
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
                    enabled = true,
                    acls = emptyList()
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
                    enabled = true,
                    acls = emptyList()
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


    private fun KubernetesOperatorContext.createAndGetDeployment(app: FlaisApplicationCrd): Deployment {
        create(app)
        await atMost Duration.ofMinutes(10) until {
            get<FlaisApplicationCrd>(app.metadata.name)?.status?.state == FlaisApplicationState.DEPLOYED
        }
        return get<Deployment>(app.metadata.name) ?: error("Deployment not found")
    }

    private fun createTestFlaisApplication() = FlaisApplicationCrd().apply {
        metadata = ObjectMeta().apply {
            name = "test"

            labels = mapOf(
                "fintlabs.no/team" to "test",
                "fintlabs.no/org-id" to "test.org",
            )
        }
        spec = FlaisApplicationSpec(
            orgId = "test.org",
            image = "test-image"
        )
    }
}