package no.fintlabs.application

import io.fabric8.kubernetes.api.model.Service
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import no.fintlabs.application.Utils.createAndGetResource
import no.fintlabs.application.Utils.createApplicationKoinTestExtension
import no.fintlabs.application.Utils.createApplicationKubernetesOperatorExtension
import no.fintlabs.application.Utils.createTestFlaisApplication
import no.fintlabs.application.api.v1alpha1.FlaisApplication
import no.fintlabs.extensions.KubernetesOperatorContext
import org.junit.jupiter.api.extension.RegisterExtension

class ServiceDRTest {
  // region General
  @Test
  fun `should create Service`(context: KubernetesOperatorContext) {
    val flaisApplication = createTestFlaisApplication()
    val service = context.createAndGetService(flaisApplication)

    assertNotNull(service)
    assertEquals(flaisApplication.metadata.name, service.metadata.name)
    assertEquals("ClusterIP", service.spec.type)
    assertEquals(1, service.spec.ports.size)
    assertEquals("TCP", service.spec.ports[0].protocol)
    assertEquals(flaisApplication.spec.port, service.spec.ports[0].port)
    assertEquals(mapOf("app" to flaisApplication.metadata.name), service.spec.selector)
  }

  @Test
  fun `should create Service with custom port`(context: KubernetesOperatorContext) {
    val flaisApplication = createTestFlaisApplication().apply { spec = spec.copy(port = 1234) }
    val service = context.createAndGetService(flaisApplication)

    assertNotNull(service)
    assertEquals(flaisApplication.spec.port, service.spec.ports[0].port)
  }

  // endregion

  private fun KubernetesOperatorContext.createAndGetService(app: FlaisApplication) =
      createAndGetResource<Service>(app)

  companion object {
    @RegisterExtension val koinTestExtension = createApplicationKoinTestExtension()

    @RegisterExtension
    val kubernetesOperatorExtension = createApplicationKubernetesOperatorExtension()
  }
}
