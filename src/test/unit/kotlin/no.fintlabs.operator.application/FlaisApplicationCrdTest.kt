package no.fintlabs.operator.application

import io.fabric8.kubernetes.api.model.IntOrString
import io.fabric8.kubernetes.api.model.apiextensions.v1.CustomResourceDefinition
import io.fabric8.kubernetes.api.model.apps.DeploymentStrategy
import io.fabric8.kubernetes.api.model.apps.RollingUpdateDeployment
import io.fabric8.kubernetes.client.KubernetesClient
import io.fabric8.kubernetes.client.dsl.base.CustomResourceDefinitionContext
import io.fabric8.kubernetes.client.server.mock.EnableKubernetesMockClient
import no.fintlabs.operator.application.api.v1alpha1.*
import no.fintlabs.v1alpha1.kafkauserandaclspec.Acls
import org.hamcrest.MatcherAssert.assertThat
import org.junit.jupiter.api.BeforeEach
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import org.hamcrest.CoreMatchers.`is` as isEqualTo

@EnableKubernetesMockClient(crud = true)
class FlaisApplicationCrdTest {

    private lateinit var client: KubernetesClient
    private lateinit var crd: CustomResourceDefinition

    @BeforeEach
    fun beforeEach() {
        CustomResourceDefinitionContext
            .v1CRDFromCustomResourceType(FlaisApplicationCrd::class.java)
            .build()
            .let {
                crd = it
                client.apiextensions().v1().customResourceDefinitions().resource(it).create()
            }

    }

    @Test
    fun `FlaisApplicationCrd should exist in the cluster`() {
        val crdList = client.apiextensions().v1().customResourceDefinitions().list();
        assert(crdList.items.size == 1)

        val crd = crdList.items.first()
        assertEquals("Application", crd.spec.names.kind)
        assertEquals("fintlabs.no", crd.spec.group)
        assertEquals("v1alpha1", crd.spec.versions.first().name)
    }

    @Test
    fun `FlaisApplicationCrd should have correct base values`() {
        // Create a FlaisApplicationCrd instance
        val origFlaisApplication = createAndApplyFlaisApplication()

        // Get the FlaisApplicationCrd instance
        val resFlaisApplication = getFlaisApplication()


        // Verify if the values are correctly applied
        assertNotNull(resFlaisApplication)
        assertEquals(origFlaisApplication.metadata.name, resFlaisApplication.metadata.name)
        assertEquals(origFlaisApplication.metadata.namespace, resFlaisApplication.metadata.namespace)

        assertEquals(origFlaisApplication.spec.orgId, resFlaisApplication.spec.orgId)
        assertEquals(origFlaisApplication.spec.port, resFlaisApplication.spec.port)
        assertEquals(origFlaisApplication.spec.image, resFlaisApplication.spec.image)
        assertEquals(origFlaisApplication.spec.imagePullPolicy, resFlaisApplication.spec.imagePullPolicy)
        assertEquals(origFlaisApplication.spec.port, resFlaisApplication.spec.port)
        assertEquals(origFlaisApplication.spec.restartPolicy, resFlaisApplication.spec.restartPolicy)
        assertEquals(origFlaisApplication.spec.replicas, resFlaisApplication.spec.replicas)
        assertEquals(origFlaisApplication.spec.env, resFlaisApplication.spec.env)

        assertThat(origFlaisApplication.spec.resources.claims, isEqualTo(resFlaisApplication.spec.resources.claims))
        assertThat(origFlaisApplication.spec.resources.limits, isEqualTo(resFlaisApplication.spec.resources.limits))
        assertThat(origFlaisApplication.spec.resources.requests, isEqualTo(resFlaisApplication.spec.resources.requests))

        assertThat(origFlaisApplication.spec.strategy, isEqualTo(resFlaisApplication.spec.strategy))
        assertThat(origFlaisApplication.spec.prometheus, isEqualTo(resFlaisApplication.spec.prometheus))
        assertThat(origFlaisApplication.spec.onePassword, isEqualTo(resFlaisApplication.spec.onePassword))
        assertThat(origFlaisApplication.spec.kafka, isEqualTo(resFlaisApplication.spec.kafka))
        assertThat(origFlaisApplication.spec.database, isEqualTo(resFlaisApplication.spec.database))
        assertThat(origFlaisApplication.spec.url, isEqualTo(resFlaisApplication.spec.url))
        assertThat(origFlaisApplication.spec.ingress, isEqualTo(resFlaisApplication.spec.ingress))
    }

    @Test
    fun `FlaisApplicationCrd should have correct deploymentStrategy`() {
        val origFlaisApplication = createAndApplyFlaisApplication(
            FlaisApplicationSpec(strategy = DeploymentStrategy().apply {
                type = "Recreate"
                rollingUpdate = RollingUpdateDeployment().apply {
                    maxSurge = IntOrString("25%")
                    maxUnavailable = IntOrString("25%")
                }
            }
            )
        )
        val resFlaisApplication = getFlaisApplication()
        assertThat(origFlaisApplication.spec.strategy, isEqualTo(resFlaisApplication.spec.strategy))
    }

    @Test
    fun `FlaisApplicationCrd should have correct prometheus`() {
        createAndApplyFlaisApplication(
            FlaisApplicationSpec(
                prometheus = Metrics(
                    enabled = true,
                    path = "/metrics/nono",
                    port = "8081"
                )
            )
        )
        val resFlaisApplication = getFlaisApplication()
        assertEquals(true, resFlaisApplication.spec.prometheus.enabled)
        assertEquals("/metrics/nono", resFlaisApplication.spec.prometheus.path)
        assertEquals("8081", resFlaisApplication.spec.prometheus.port)
    }

    @Test
    fun `FlaisApplicationCrd should have correct onePassword`() {
        createAndApplyFlaisApplication(
            FlaisApplicationSpec(onePassword = OnePassword("test-itemPath"))
        )
        val resFlaisApplication = getFlaisApplication()
        assertNotNull(resFlaisApplication.spec.onePassword)
        assertEquals("test-itemPath", resFlaisApplication.spec.onePassword?.itemPath)
    }

    @Test
    fun `FlaisApplicationCrd should have correct kafka`() {
        createAndApplyFlaisApplication(
            FlaisApplicationSpec(
                kafka = Kafka(
                    enabled = true,
                    acls = listOf(Acls().apply {
                        topic = "test-resource"
                        permission = "test-permission"
                    })
                )
            )
        )
        val resFlaisApplication = getFlaisApplication()
        assertEquals(true, resFlaisApplication.spec.kafka.enabled)
        assertEquals(1, resFlaisApplication.spec.kafka.acls.size)
        assertEquals("test-resource", resFlaisApplication.spec.kafka.acls.first().topic)
        assertEquals("test-permission", resFlaisApplication.spec.kafka.acls.first().permission)
    }

    @Test
    fun `FlaisApplicationCrd should have correct database`() {
        createAndApplyFlaisApplication(
            FlaisApplicationSpec(
                database = Database("test-database")
            )
        )
        val resFlaisApplication = getFlaisApplication()
        @Suppress("DEPRECATION")
        assertEquals(true, resFlaisApplication.spec.database.enabled)
        assertEquals("test-database", resFlaisApplication.spec.database.database)
    }

    @Test
    fun `FlaisApplicationCrd should have correct url`() {
        createAndApplyFlaisApplication(
            FlaisApplicationSpec(
                url = Url(
                    basePath = "/test-path",
                    hostname = "test-hostname"
                )
            )
        )
        val resFlaisApplication = getFlaisApplication()
        assertEquals("/test-path", resFlaisApplication.spec.url.basePath)
        assertEquals("test-hostname", resFlaisApplication.spec.url.hostname)
    }

    @Test
    fun `FlaisApplicationCrd should have correct ingress`() {
        createAndApplyFlaisApplication(
            FlaisApplicationSpec(
                ingress = Ingress(
                    enabled = true,
                    basePath = "/test-path",
                    middlewares = listOf("test-middleware")
                )
            )
        )
        val resFlaisApplication = getFlaisApplication()
        assertEquals(true, resFlaisApplication.spec.ingress.enabled)
        assertEquals("/test-path", resFlaisApplication.spec.ingress.basePath)
        assertEquals(1, resFlaisApplication.spec.ingress.middlewares.size)
        assertEquals("test-middleware", resFlaisApplication.spec.ingress.middlewares.first())
    }


    private fun createAndApplyFlaisApplication(spec: FlaisApplicationSpec = FlaisApplicationSpec()) = FlaisApplicationCrd().apply {
        metadata.name = "test-application"
        metadata.namespace = "default"
        this.spec = spec.copy(
            orgId = "default-orgId",
            image = "default-image",
            imagePullSecrets = listOf("default-imagePullSecrets"),
        )
    }.also {
        client.resource(it).create()
    }

    private fun getFlaisApplication(): FlaisApplicationCrd {
        return client.resources(FlaisApplicationCrd::class.java)
            .inNamespace("default")
            .withName("test-application")
            .get()
    }
}