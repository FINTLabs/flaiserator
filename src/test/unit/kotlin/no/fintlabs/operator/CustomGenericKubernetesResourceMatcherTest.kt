package no.fintlabs.operator

import io.fabric8.kubernetes.api.model.HasMetadata
import io.fabric8.kubernetes.api.model.PersistentVolume
import io.fabric8.kubernetes.api.model.apps.Deployment
import io.fabric8.kubernetes.api.model.apps.ReplicaSet
import io.fabric8.kubernetes.api.model.batch.v1.Job
import io.fabric8.kubernetes.client.utils.KubernetesSerialization
import io.javaoperatorsdk.operator.ReconcilerUtils
import io.javaoperatorsdk.operator.api.reconciler.DefaultContext
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import no.fintlabs.operator.matcher.CustomGenericKubernetesResourceMatcher
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.extension.ExtendWith
import kotlin.test.Test
import kotlin.test.assertTrue

@ExtendWith(MockKExtension::class)
class CustomGenericKubernetesResourceMatcherTest {
    @MockK
    lateinit var context: DefaultContext<HasMetadata>

    @BeforeEach
    fun setup() {
        val objectMapper = KubernetesSerialization()
        every { context.client.kubernetesSerialization } returns objectMapper
        every { context.controllerConfiguration.fieldManager() } returns "controller"
    }

    @Test
    fun `test deployment with different resource value representation`() {
        val matcher = CustomGenericKubernetesResourceMatcher.getInstance<Deployment>()
        val actual = loadResource<Deployment>("/resource-matcher/kubernetes/simple-deployment.yaml")
        val desired = loadResource<Deployment>("/resource-matcher/kubernetes/simple-deployment-desired.yaml")
        val match = matcher.matches(actual, desired, context)

        assertTrue(match)
    }

    @Test
    fun `test job with different resource value representation`() {
        val matcher = CustomGenericKubernetesResourceMatcher.getInstance<Job>()
        val actual = loadResource<Job>("/resource-matcher/kubernetes/simple-job.yaml")
        val desired = loadResource<Job>("/resource-matcher/kubernetes/simple-job-desired.yaml")
        val match = matcher.matches(actual, desired, context)

        assertTrue(match)
    }

    @Test
    fun `test replica set with different resource value representation`() {
        val matcher = CustomGenericKubernetesResourceMatcher.getInstance<ReplicaSet>()
        val actual = loadResource<ReplicaSet>("/resource-matcher/kubernetes/simple-replica-set.yaml")
        val desired = loadResource<ReplicaSet>("/resource-matcher/kubernetes/simple-replica-set-desired.yaml")
        val match = matcher.matches(actual, desired, context)

        assertTrue(match)
    }

    @Test
    fun `test persistent volume with different capacity value representation`() {
        val matcher = CustomGenericKubernetesResourceMatcher.getInstance<PersistentVolume>()
        val actual = loadResource<PersistentVolume>("/resource-matcher/kubernetes/simple-persistent-volume.yaml")
        val desired = loadResource<PersistentVolume>("/resource-matcher/kubernetes/simple-persistent-volume-desired.yaml")
        val match = matcher.matches(actual, desired, context)

        assertTrue(match)
    }

    private inline fun <reified T> loadResource(path: String): T {
        return ReconcilerUtils.loadYaml(T::class.java, T::class.java, path)
    }
}