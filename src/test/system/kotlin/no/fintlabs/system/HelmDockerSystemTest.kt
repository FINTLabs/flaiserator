package no.fintlabs.system

import io.fabric8.kubernetes.api.model.NamespaceBuilder
import no.fintlabs.application.api.v1alpha1.FlaisApplication
import no.fintlabs.common.api.v1alpha1.FlaisResourceState
import no.fintlabs.job.api.v1alpha1.FlaisJob
import org.awaitility.kotlin.await
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import java.time.Duration
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Execution(ExecutionMode.SAME_THREAD)
class HelmDockerSystemTest {
    private val cluster = SystemTestCluster()
    private val client get() = cluster.client
    private val timeout = Duration.ofMinutes(3)

    @BeforeAll
    fun installOperator() = cluster.start()

    @AfterAll
    fun deleteCluster() = cluster.close()

    @Test
    fun `Helm installs the locally built Docker image and the operator becomes ready`() {
        val deployment =
            client
                .apps()
                .deployments()
                .inNamespace("flais-system")
                .withName("flaiserator")
                .get()
        assertEquals(
            "flaiserator:system-test",
            deployment.spec.template.spec.containers
                .single()
                .image,
        )
        assertEquals(
            "Never",
            deployment.spec.template.spec.containers
                .single()
                .imagePullPolicy,
        )
        assertEquals(1, deployment.status.availableReplicas)
        assertTrue(
            client
                .apiextensions()
                .v1()
                .customResourceDefinitions()
                .withName("applications.fintlabs.no")
                .get()
                .status.conditions
                .any { it.type == "Established" && it.status == "True" },
        )
    }

    @Test
    fun `Application serves HTTP through its Service and generated resources are garbage collected`() {
        val namespace = "system-test-app"
        applyFixture(namespace, "application.yaml")
        val application = client.resources(FlaisApplication::class.java).inNamespace(namespace).withName("system-test-app")
        val deployment =
            client
                .apps()
                .deployments()
                .inNamespace(namespace)
                .withName("system-test-app")
        val service = client.services().inNamespace(namespace).withName("system-test-app")

        await.atMost(timeout).untilAsserted {
            assertEquals(FlaisResourceState.DEPLOYED, application.get()?.status?.state)
            assertEquals(1, deployment.get()?.status?.availableReplicas)
            assertEquals("ClusterIP", service.get()?.spec?.type)
        }
        await.atMost(timeout).untilAsserted {
            assertEquals(
                "flaiserator-system-test",
                cluster.kubectl(
                    "-n",
                    namespace,
                    "exec",
                    "deployment/system-test-app",
                    "--",
                    "wget",
                    "-T",
                    "10",
                    "-qO-",
                    "http://system-test-app:8080",
                ),
            )
        }

        application.delete()
        await.atMost(timeout).untilAsserted {
            assertNull(application.get())
            assertNull(deployment.get())
            assertNull(service.get())
        }
    }

    @Test
    fun `one-shot Job completes successfully and generated resources are garbage collected`() {
        val namespace = "system-test-job"
        applyFixture(namespace, "job.yaml")
        val resource = client.resources(FlaisJob::class.java).inNamespace(namespace).withName("system-test-job")
        val job =
            client
                .batch()
                .v1()
                .jobs()
                .inNamespace(namespace)
                .withName("system-test-job-1")
        val cronJob =
            client
                .batch()
                .v1()
                .cronjobs()
                .inNamespace(namespace)
                .withName("system-test-job")

        await.atMost(timeout).untilAsserted {
            assertEquals(1, job.get()?.status?.succeeded)
            assertEquals(FlaisResourceState.DEPLOYED, resource.get()?.status?.state)
        }
        assertEquals("flaiserator-system-test-job-completed", job.log.trim())
        assertEquals(true, cronJob.get().spec.suspend)

        resource.delete()
        await.atMost(timeout).untilAsserted {
            assertNull(resource.get())
            assertNull(cronJob.get())
            assertNull(job.get())
        }
    }

    private fun applyFixture(
        namespace: String,
        fixture: String,
    ) {
        client
            .namespaces()
            .resource(
                NamespaceBuilder()
                    .withNewMetadata()
                    .withName(namespace)
                    .endMetadata()
                    .build(),
            ).create()
        val input = checkNotNull(javaClass.getResourceAsStream("/workloads/$fixture"))
        input.use { client.load(it).inNamespace(namespace).create() }
    }
}
