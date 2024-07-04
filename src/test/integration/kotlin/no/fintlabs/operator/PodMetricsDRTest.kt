package no.fintlabs.operator

import com.coreos.monitoring.v1.PodMonitor
import no.fintlabs.extensions.KubernetesOperatorContext
import no.fintlabs.operator.Utils.createAndGetResource
import no.fintlabs.operator.Utils.createKoinTestExtension
import no.fintlabs.operator.Utils.createKubernetesOperatorExtension
import no.fintlabs.operator.Utils.createTestFlaisApplication
import no.fintlabs.operator.api.v1alpha1.FlaisApplicationCrd
import no.fintlabs.operator.api.v1alpha1.Metrics
import no.fintlabs.operator.api.v1alpha1.Observability
import org.junit.jupiter.api.extension.RegisterExtension
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class PodMetricsDRTest {
    //region General
    @Test
    fun `should create PodMonitor`(context: KubernetesOperatorContext) {
        val flaisApplication = createTestFlaisApplication().apply {
            spec = spec.copy(observability = Observability(metrics = Metrics(
                enabled = true,
                path = "/actuator/prometheus",
                port = "8080"
            )))
        }

        val podMonitor = context.createAndGetPodMonitor(flaisApplication)
        assertNotNull(podMonitor)
        assertEquals("test", podMonitor.metadata.name)
        assertEquals("test", podMonitor.spec.selector.matchLabels["app"])
    }

    @Test
    fun `should create PodMonitor with correct path and port`(context: KubernetesOperatorContext) {
        val flaisApplication = createTestFlaisApplication().apply {
            spec = spec.copy(observability = Observability(metrics = Metrics(
                enabled = true,
                path = "/metrics",
                port = "1234"
            )))
        }

        val podMonitor = context.createAndGetPodMonitor(flaisApplication)
        assertNotNull(podMonitor)
        assertEquals("/metrics", podMonitor.spec.podMetricsEndpoints[0].path)
        assertEquals("1234", podMonitor.spec.podMetricsEndpoints[0].targetPort.strVal)
    }

    @Test
    fun `should not create PodMonitor when metrics is disabled`(context: KubernetesOperatorContext) {
        val flaisApplication = createTestFlaisApplication().apply {
            spec = spec.copy(observability = Observability(metrics = Metrics(
                enabled = false,
                path = "/actuator/prometheus",
                port = "8080"
            )))
        }

        val podMonitor = context.createAndGetPodMonitor(flaisApplication)
        assertNull(podMonitor)
    }
    //endregion

    private fun KubernetesOperatorContext.createAndGetPodMonitor(app: FlaisApplicationCrd) =
        createAndGetResource<PodMonitor>(app) { it.metadata.name }

    companion object {
        @RegisterExtension
        val koinTestExtension = createKoinTestExtension()

        @RegisterExtension
        val kubernetesOperatorExtension = createKubernetesOperatorExtension()
    }
}