package no.fintlabs.application

import io.mockk.every
import io.mockk.spyk
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import no.fintlabs.application.Utils.createAndGetResource
import no.fintlabs.application.Utils.createKoinTestExtension
import no.fintlabs.application.Utils.createKubernetesOperatorExtension
import no.fintlabs.application.Utils.createTestFlaisApplication
import no.fintlabs.application.Utils.waitUntil
import no.fintlabs.application.api.v1alpha1.FlaisApplicationCrd
import no.fintlabs.application.api.v1alpha1.FlaisApplicationState
import no.fintlabs.extensions.KubernetesOperator
import no.fintlabs.extensions.KubernetesOperatorContext
import org.junit.jupiter.api.extension.RegisterExtension
import org.koin.core.component.get
import org.koin.test.KoinTest
import org.koin.test.mock.declare

class ApplicationReconcilerTest : KoinTest {

  @Test
  fun `should set correlation id on FlaisApplication`(context: KubernetesOperatorContext) {
    val flaisApplication = createTestFlaisApplication()
    val app = context.createAndGetApplication(flaisApplication)

    assertNotNull(app)
    assertContains(app.metadata.annotations, "fintlabs.no/deployment-correlation-id")
    assertNotNull(app.status.correlationId)
    assertEquals(
        app.metadata.annotations["fintlabs.no/deployment-correlation-id"],
        app.status.correlationId,
    )
  }

  @Test
  fun `should not set correlation id on FlaisApplication if exists`(
      context: KubernetesOperatorContext
  ) {
    val flaisApplication =
        createTestFlaisApplication().apply {
          metadata.annotations["fintlabs.no/deployment-correlation-id"] = "123"
        }
    val app = context.createAndGetApplication(flaisApplication)

    assertNotNull(app)
    assertContains(app.metadata.annotations, "fintlabs.no/deployment-correlation-id")
    assertEquals("123", app.metadata.annotations["fintlabs.no/deployment-correlation-id"])
    assertNotNull(app.status.correlationId)
    assertEquals("123", app.status.correlationId)
  }

  @KubernetesOperator(explicitStart = true)
  @Test
  fun `should handle dependent errors`(context: KubernetesOperatorContext) {
    val service = spyk(get<ServiceDR>())
    every { service.reconcile(any(), any()) } throws RuntimeException("test")

    declare<ServiceDR> { service }

    context.operator.start()

    val flaisApplication = createTestFlaisApplication()
    context.create(flaisApplication)
    context.waitUntil<FlaisApplicationCrd>(flaisApplication.metadata.name) {
      it.status !== null && it.status?.state != FlaisApplicationState.PENDING
    }
    val app = context.get<FlaisApplicationCrd>(flaisApplication.metadata.name)

    assertNotNull(app)
    assertEquals(1, app.status.errors?.size)
    assertEquals("test", app.status.errors?.find { it.dependent == service.name() }?.message)
  }

  private fun KubernetesOperatorContext.createAndGetApplication(app: FlaisApplicationCrd) =
      createAndGetResource<FlaisApplicationCrd>(app)

  companion object {
    @RegisterExtension val koinTestExtension = createKoinTestExtension()

    @RegisterExtension val kubernetesOperatorExtension = createKubernetesOperatorExtension()
  }
}
