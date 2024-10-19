package no.fintlabs.operator

import no.fintlabs.extensions.KubernetesOperatorContext
import no.fintlabs.operator.Utils.createAndGetResource
import no.fintlabs.operator.Utils.createKoinTestExtension
import no.fintlabs.operator.Utils.createKubernetesOperatorExtension
import no.fintlabs.operator.Utils.createTestFlaisApplication
import no.fintlabs.operator.Utils.waitUntil
import no.fintlabs.operator.api.v1alpha1.FlaisApplicationCrd
import no.fintlabs.operator.api.v1alpha1.FlaisApplicationState
import org.junit.jupiter.api.extension.RegisterExtension
import org.koin.core.component.inject
import org.koin.test.KoinTest
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class FlaisApplicationReconcilerTest : KoinTest {

    @Test
    fun `should set correlation id on FlaisApplication`(context: KubernetesOperatorContext) {
        val flaisApplication = createTestFlaisApplication()
        val app = context.createAndGetApplication(flaisApplication)

        assertNotNull(app)
        assertContains(app.metadata.annotations, "fintlabs.no/deployment-correlation-id")
        assertNotNull(app.status.correlationId)
        assertEquals(app.metadata.annotations["fintlabs.no/deployment-correlation-id"], app.status.correlationId)
    }

    @Test
    fun `should not set correlation id on FlaisApplication if exists`(context: KubernetesOperatorContext) {
        val flaisApplication = createTestFlaisApplication().apply {
            metadata.annotations["fintlabs.no/deployment-correlation-id"] = "123"
        }
        val app = context.createAndGetApplication(flaisApplication)

        assertNotNull(app)
        assertContains(app.metadata.annotations, "fintlabs.no/deployment-correlation-id")
        assertEquals("123", app.metadata.annotations["fintlabs.no/deployment-correlation-id"])
        assertNotNull(app.status.correlationId)
        assertEquals("123", app.status.correlationId)
    }

    @Test
    fun `should handle dependent errors`(context: KubernetesOperatorContext) {
        val service: ServiceDR by inject()
        service.setResourceDiscriminator { _, _, _ ->
            throw RuntimeException("test")
        }

        val flaisApplication = createTestFlaisApplication()
        context.create(flaisApplication)
        context.waitUntil<FlaisApplicationCrd>(flaisApplication.metadata.name) { it.status.state != FlaisApplicationState.PENDING }
        val app = context.get<FlaisApplicationCrd>(flaisApplication.metadata.name)

        assertNotNull(app)
        assertEquals(1, app.status.dependentErrors?.size)
        assertEquals("test", app.status.dependentErrors?.get("Service"))
    }


    private fun KubernetesOperatorContext.createAndGetApplication(app: FlaisApplicationCrd) =
        createAndGetResource<FlaisApplicationCrd>(app)

    companion object {
        @RegisterExtension
        val koinTestExtension = createKoinTestExtension()

        @RegisterExtension
        val kubernetesOperatorExtension = createKubernetesOperatorExtension()
    }
}