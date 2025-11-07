package no.fintlabs.application

import com.coreos.monitoring.v1.PodMonitor
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import no.fintlabs.application.Utils.createAndGetResource
import no.fintlabs.application.Utils.createKoinTestExtension
import no.fintlabs.application.Utils.createKubernetesOperatorExtension
import no.fintlabs.application.Utils.createTestFlaisApplication
import no.fintlabs.application.api.v1alpha1.FlaisApplication
import no.fintlabs.application.api.v1alpha1.Metrics
import no.fintlabs.application.api.v1alpha1.ApplicationObservability
import no.fintlabs.extensions.KubernetesOperatorContext
import org.junit.jupiter.api.extension.RegisterExtension

class PodMetricsDRTest {
  // region General
  @Test
  fun `should create PodMonitor`(context: KubernetesOperatorContext) {
    val flaisApplication =
        createTestFlaisApplication().apply {
          spec =
              spec.copy(
                  observability =
                      ApplicationObservability(
                          metrics =
                              Metrics(enabled = true, path = "/actuator/prometheus", port = "8080")
                      )
              )
        }

    val podMonitor = context.createAndGetPodMonitor(flaisApplication)
    assertNotNull(podMonitor)
    assertEquals("test", podMonitor.metadata.name)
    assertEquals("test", podMonitor.spec.selector.matchLabels["app"])
    assertEquals("/actuator/prometheus", podMonitor.spec.podMetricsEndpoints[0].path)
    assertEquals("http", podMonitor.spec.podMetricsEndpoints[0].port)
  }

  @Test
  fun `should create PodMonitor with correct path and port`(context: KubernetesOperatorContext) {
    val flaisApplication =
        createTestFlaisApplication().apply {
          spec =
              spec.copy(
                  observability =
                      ApplicationObservability(
                          metrics = Metrics(enabled = true, path = "/metrics", port = "1234")
                      )
              )
        }

    val podMonitor = context.createAndGetPodMonitor(flaisApplication)
    assertNotNull(podMonitor)
    assertEquals("/metrics", podMonitor.spec.podMetricsEndpoints[0].path)
    assertEquals("metrics", podMonitor.spec.podMetricsEndpoints[0].port)
  }

  @Test
  fun `should not create PodMonitor when metrics is disabled`(context: KubernetesOperatorContext) {
    val flaisApplication =
        createTestFlaisApplication().apply {
          spec =
              spec.copy(
                  observability =
                      ApplicationObservability(
                          metrics =
                              Metrics(enabled = false, path = "/actuator/prometheus", port = "8080")
                      )
              )
        }

    val podMonitor = context.createAndGetPodMonitor(flaisApplication)
    assertNull(podMonitor)
  }

  // endregion

  private fun KubernetesOperatorContext.createAndGetPodMonitor(app: FlaisApplication) =
      createAndGetResource<PodMonitor>(app) { it.metadata.name }

  companion object {
    @RegisterExtension val koinTestExtension = createKoinTestExtension()

    @RegisterExtension val kubernetesOperatorExtension = createKubernetesOperatorExtension()
  }
}
