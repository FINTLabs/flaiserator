package no.fintlabs.operator.application

import io.fabric8.kubernetes.api.model.IntOrString
import io.fabric8.kubernetes.api.model.apiextensions.v1.CustomResourceDefinition
import io.fabric8.kubernetes.api.model.apps.DeploymentStrategy
import io.fabric8.kubernetes.api.model.apps.RollingUpdateDeployment
import io.fabric8.kubernetes.client.KubernetesClient
import io.fabric8.kubernetes.client.dsl.base.CustomResourceDefinitionContext
import io.fabric8.kubernetes.client.server.mock.EnableKubernetesMockClient
import no.fintlabs.operator.application.api.FlaisApplicationCrd
import no.fintlabs.operator.application.api.FlaisApplicationSpec
import no.fintlabs.operator.application.api.OnePassword
import no.fintlabs.v1alpha1.kafkauserandaclspec.Acls
import org.hamcrest.MatcherAssert.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.TestInstance
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import org.hamcrest.CoreMatchers.`is` as isEqualTo

@EnableKubernetesMockClient(crud = true)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class FlaisApplicationCrdTest {

    lateinit var client: KubernetesClient
    lateinit var crd: CustomResourceDefinition

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
    fun `FlaisApplicationCrd should exist in the kluster`() {
        val crdList = client.apiextensions().v1().customResourceDefinitions().list();
        assert(crdList.items.size == 1)
        assert(crdList.items.first().spec.names.kind == "FlaisApplication")
        assert(crdList.items.first().spec.group == "fintlabs.no")
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

        assertThat(origFlaisApplication.spec.deploymentStrategy, isEqualTo(resFlaisApplication.spec.deploymentStrategy))
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
            FlaisApplicationSpec().apply {
                deploymentStrategy = DeploymentStrategy().apply {
                    type = "Recreate"
                    rollingUpdate = RollingUpdateDeployment().apply {
                        maxSurge = IntOrString("25%")
                        maxUnavailable = IntOrString("25%")
                    }
                }
            }
        )
        val resFlaisApplication = getFlaisApplication()
        assertThat(origFlaisApplication.spec.deploymentStrategy, isEqualTo(resFlaisApplication.spec.deploymentStrategy))
    }

    @Test
    fun `FlaisApplicationCrd should have correct prometheus`() {
        createAndApplyFlaisApplication(
            FlaisApplicationSpec().apply {
                prometheus.apply {
                    enabled = true
                    path = "/metrics/nono"
                    port = "8081"
                }
            }
        )
        val resFlaisApplication = getFlaisApplication()
        assertEquals(true, resFlaisApplication.spec.prometheus.enabled)
        assertEquals("/metrics/nono", resFlaisApplication.spec.prometheus.path)
        assertEquals("8081", resFlaisApplication.spec.prometheus.port)
    }

    @Test
    fun `FlaisApplicationCrd should have correct onePassword`() {
        createAndApplyFlaisApplication(
            FlaisApplicationSpec().apply {
                onePassword = OnePassword().apply {
                    itemPath = "test-itemPath"
                }
            }
        )
        val resFlaisApplication = getFlaisApplication()
        assertNotNull(resFlaisApplication.spec.onePassword)
        assertEquals("test-itemPath", resFlaisApplication.spec.onePassword?.itemPath)
    }

    @Test
    fun `FlaisApplicationCrd should have correct kafka`() {
        createAndApplyFlaisApplication(
            FlaisApplicationSpec().apply {
                kafka.apply {
                    enabled = true
                    acls = listOf(Acls().apply {
                        topic = "test-resource"
                        permission = "test-permission"
                    })
                }
            }
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
            FlaisApplicationSpec().apply {
                database.apply {
                    enabled = true
                    database = "test-database"
                }
            }
        )
        val resFlaisApplication = getFlaisApplication()
        assertEquals(true, resFlaisApplication.spec.database.enabled)
        assertEquals("test-database", resFlaisApplication.spec.database.database)
    }

    @Test
    fun `FlaisApplicationCrd should have correct url`() {
        createAndApplyFlaisApplication(
            FlaisApplicationSpec().apply {
                url.apply {
                    basePath = "/test-path"
                    hostname = "test-hostname"
                }
            }
        )
        val resFlaisApplication = getFlaisApplication()
        assertEquals("/test-path", resFlaisApplication.spec.url.basePath)
        assertEquals("test-hostname", resFlaisApplication.spec.url.hostname)
    }

    @Test
    fun `FlaisApplicationCrd should have correct ingress`() {
        createAndApplyFlaisApplication(
            FlaisApplicationSpec().apply {
                ingress.apply {
                    enabled = true
                    basePath = "/test-path"
                    middlewares = listOf("test-middleware")
                }
            }
        )
        val resFlaisApplication = getFlaisApplication()
        assertEquals(true, resFlaisApplication.spec.ingress.enabled)
        assertEquals("/test-path", resFlaisApplication.spec.ingress.basePath)
        assertEquals(1, resFlaisApplication.spec.ingress.middlewares.size)
        assertEquals("test-middleware", resFlaisApplication.spec.ingress.middlewares.first())
    }



    private fun createAndApplyFlaisApplication(spec: FlaisApplicationSpec = FlaisApplicationSpec()): FlaisApplicationCrd {
        val application = FlaisApplicationCrd().apply {
            metadata.name = "test-application"
            metadata.namespace = "default"
            this.spec = spec.apply {
                orgId = "default-orgId"
                image = "default-image"
            }
        }
        client.resource(application).create()
        return application
    }

    private fun getFlaisApplication(): FlaisApplicationCrd {
        return client.resources(FlaisApplicationCrd::class.java)
            .inNamespace("default")
            .withName("test-application")
            .get()
    }
}