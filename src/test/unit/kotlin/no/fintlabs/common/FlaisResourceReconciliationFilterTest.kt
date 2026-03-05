package no.fintlabs.common

import io.fabric8.kubernetes.api.model.ObjectMeta
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import no.fintlabs.common.api.v1alpha1.FlaisResourceState
import no.fintlabs.common.api.v1alpha1.FlaisResourceStatus
import no.fintlabs.common.api.v1alpha1.resourceHash

class FlaisResourceReconciliationFilterTest {
  @Test
  fun `should not reconcile when resource matches status`() {
    val resource = buildResource()
    resource.status = buildStatus(resource)

    assertFalse(FlaisResourceReconciliationFilter.shouldReconcile(resource))
  }

  @Test
  fun `should reconcile when synchronization hash differs`() {
    val resource = buildResource()
    resource.status = buildStatus(resource, synchronizationHash = 123)

    assertTrue(FlaisResourceReconciliationFilter.shouldReconcile(resource))
  }

  @Test
  fun `should reconcile when correlation id differs`() {
    val resource = buildResource(correlationIdAnnotation = "corr-new")
    resource.status = buildStatus(resource, correlationId = "corr-old")

    assertTrue(FlaisResourceReconciliationFilter.shouldReconcile(resource))
  }

  @Test
  fun `should reconcile when observed generation differs`() {
    val resource = buildResource(generation = 2)
    resource.status = buildStatus(resource, observedGeneration = 1)

    assertTrue(FlaisResourceReconciliationFilter.shouldReconcile(resource))
  }

  @Test
  fun `should reconcile when status is pending`() {
    val resource = buildResource()
    resource.status = buildStatus(resource, state = FlaisResourceState.PENDING)

    assertTrue(FlaisResourceReconciliationFilter.shouldReconcile(resource))
  }

  @Test
  fun `should reconcile when status is missing`() {
    val resource = buildResource()

    assertTrue(FlaisResourceReconciliationFilter.shouldReconcile(resource))
  }

  @Test
  fun `should reconcile when correlation id annotation is missing`() {
    val resource = buildResource(correlationIdAnnotation = null)
    resource.status = buildStatus(resource, correlationId = "corr")

    assertTrue(FlaisResourceReconciliationFilter.shouldReconcile(resource))
  }

  private fun buildResource(
      generation: Long = 1,
      correlationIdAnnotation: String? = "corr",
  ): TestResource =
      TestResource().apply {
        metadata =
            ObjectMeta().apply {
              name = "test"
              labels = mutableMapOf("fintlabs.no/team" to "alpha")
              this.generation = generation
              annotations =
                  correlationIdAnnotation?.let {
                    mutableMapOf("fintlabs.no/deployment-correlation-id" to it)
                  }
            }
        spec = TestSpec()
      }

  private fun buildStatus(
      resource: TestResource,
      observedGeneration: Long = resource.metadata.generation,
      state: FlaisResourceState = FlaisResourceState.DEPLOYED,
      correlationId: String? =
          resource.metadata.annotations["fintlabs.no/deployment-correlation-id"],
      synchronizationHash: Int = resource.resourceHash(),
  ): FlaisResourceStatus =
      FlaisResourceStatus(
          observedGeneration = observedGeneration,
          state = state,
          correlationId = correlationId,
          synchronizationHash = synchronizationHash,
      )
}
