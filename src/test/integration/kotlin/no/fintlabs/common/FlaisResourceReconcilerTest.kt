package no.fintlabs.common

import io.javaoperatorsdk.operator.api.reconciler.Reconciler
import io.javaoperatorsdk.operator.processing.retry.GenericRetry
import io.mockk.every
import io.mockk.spyk
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import no.fintlabs.Utils.waitUntil
import no.fintlabs.common.Utils.createAndGetResource
import no.fintlabs.common.Utils.createKoinTestExtension
import no.fintlabs.common.Utils.createKubernetesOperatorExtension
import no.fintlabs.common.Utils.createTestResource
import no.fintlabs.common.api.v1alpha1.FlaisResourceState
import no.fintlabs.extensions.KubernetesOperator
import no.fintlabs.extensions.KubernetesOperatorContext
import org.junit.jupiter.api.extension.RegisterExtension
import org.koin.core.component.get
import org.koin.core.qualifier.named
import org.koin.test.KoinTest
import org.koin.test.mock.declare

class FlaisResourceReconcilerTest : KoinTest {
  @Test
  fun `should set correlation id on FlaisResource`(context: KubernetesOperatorContext) {
    val testResource = context.createAndGetTestResource(createTestResource())

    assertNotNull(testResource)
    assertContains(testResource.metadata.annotations, "fintlabs.no/deployment-correlation-id")
    assertNotNull(testResource.status.correlationId)
    assertEquals(
        testResource.metadata.annotations["fintlabs.no/deployment-correlation-id"],
        testResource.status.correlationId,
    )
  }

  @Test
  fun `should not set correlation id on FlaisResource if exists`(
      context: KubernetesOperatorContext
  ) {
    val testResource =
        context.createAndGetTestResource(
            createTestResource().apply {
              metadata.annotations["fintlabs.no/deployment-correlation-id"] = "123"
            }
        )

    assertNotNull(testResource)
    assertContains(testResource.metadata.annotations, "fintlabs.no/deployment-correlation-id")
    assertEquals("123", testResource.metadata.annotations["fintlabs.no/deployment-correlation-id"])
    assertNotNull(testResource.status.correlationId)
    assertEquals("123", testResource.status.correlationId)
  }

  @KubernetesOperator(explicitStart = true, registerReconcilers = false)
  @Test
  fun `should handle dependent errors`(context: KubernetesOperatorContext) {
    val service = spyk(get<TestConfigDR>())
    every { service.reconcile(any(), any()) } throws RuntimeException("error")

    declare<TestConfigDR> { service }

    val testReconciler = get<Reconciler<*>>(named("test-reconciler")) as TestReconciler
    context.registerReconciler(testReconciler) { it.withRetry(GenericRetry.noRetry()) }

    context.operator.start()

    val testResource = createTestResource()

    context.create(testResource)
    context.waitUntil<FlaisTestResource>(testResource.metadata.name) {
      it.status !== null && it.status?.state != FlaisResourceState.PENDING
    }

    val actualTestResource = context.get<FlaisTestResource>(testResource.metadata.name)
    assertNotNull(actualTestResource)
    assertEquals(1, actualTestResource.status.errors?.size)
    assertEquals(
        "error",
        actualTestResource.status.errors?.find { it.dependent == service.name() }?.message,
    )
  }

  companion object {
    private fun KubernetesOperatorContext.createAndGetTestResource(resource: FlaisTestResource) =
        createAndGetResource<FlaisTestResource>(resource)

    @RegisterExtension val koinTestExtension = createKoinTestExtension()

    @RegisterExtension val kubernetesOperatorExtension = createKubernetesOperatorExtension()
  }
}
