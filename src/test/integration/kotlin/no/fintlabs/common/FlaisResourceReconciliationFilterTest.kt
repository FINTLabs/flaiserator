package no.fintlabs.common

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.time.Duration.Companion.seconds
import nl.altindag.log.LogCaptor
import no.fintlabs.Utils.updateAndGetResource
import no.fintlabs.common.api.v1alpha1.resourceHash
import no.fintlabs.extensions.KubernetesOperatorContext
import org.awaitility.kotlin.atMost
import org.awaitility.kotlin.await
import org.awaitility.kotlin.until
import org.junit.jupiter.api.extension.RegisterExtension
import org.koin.test.KoinTest

class FlaisResourceReconciliationFilterTest : KoinTest {
  @Test
  fun `should not reconcile non updated resources on startup`(context: KubernetesOperatorContext) {
    context.createAndGetTestResource(createTestResource())
    val logsCaptor = captureOperatorLogs()

    restartOperator(context)

    await atMost 5.seconds until { logsCaptor.hasDebugMessage("Skipping event handling resource") }
  }

  @Test
  fun `should reconcile updated resources on startup`(context: KubernetesOperatorContext) {
    val resource = context.createAndGetTestResource(createTestResource())
    assertNotNull(resource)
    val logsCaptor = captureReconcilerLogs()

    context.operator.stop()
    context.update(resource.apply { spec = spec.copy(image = "123") })

    context.operator.start()
    awaitReconcile(logsCaptor)
  }

  @Test
  fun `should reconcile when labels change on startup`(context: KubernetesOperatorContext) {
    val resource = context.createAndGetTestResource(createTestResource())
    assertNotNull(resource)
    val logsCaptor = captureReconcilerLogs()

    context.operator.stop()
    resource.metadata.labels["fintlabs.no/team"] = "updated"
    context.update(resource)

    context.operator.start()
    awaitReconcile(logsCaptor)
  }

  @Test
  fun `should reconcile when correlation id changes on startup`(
      context: KubernetesOperatorContext
  ) {
    val resource = context.createAndGetTestResource(createTestResource())
    assertNotNull(resource)
    val logsCaptor = captureReconcilerLogs()

    context.operator.stop()
    resource.metadata.annotations["fintlabs.no/deployment-correlation-id"] = "updated"
    context.update(resource)

    context.operator.start()
    awaitReconcile(logsCaptor)
  }

  @Test
  fun `should reconcile when labels change`(context: KubernetesOperatorContext) {
    val testResource = context.createAndGetTestResource(createTestResource())

    assertNotNull(testResource)
    val originalHash = testResource.status.synchronizationHash
    assertNotNull(originalHash)
    assertEquals(testResource.resourceHash(), originalHash)

    testResource.metadata.labels["new-label"] = "a-new-label"

    val updatedResource = context.updateAndGetTestResource(testResource)

    assertNotNull(updatedResource)
    assertNotNull(updatedResource.status.synchronizationHash)
    assertEquals(updatedResource.resourceHash(), updatedResource.status.synchronizationHash)
    assertNotEquals(originalHash, updatedResource.status.synchronizationHash)
  }

  companion object {
    private fun KubernetesOperatorContext.createAndGetTestResource(resource: FlaisTestResource) =
        createAndGetResource<FlaisTestResource>(resource)

    private fun KubernetesOperatorContext.updateAndGetTestResource(resource: FlaisTestResource) =
        updateAndGetResource<FlaisTestResource, FlaisTestResource>(resource)

    private fun captureOperatorLogs() =
        LogCaptor.forName("io.javaoperatorsdk").apply { setLogLevelToDebug() }

    private fun captureReconcilerLogs() =
        LogCaptor.forName("no.fintlabs.common.FlaisResourceReconciler").apply {
          setLogLevelToDebug()
        }

    private fun restartOperator(context: KubernetesOperatorContext) {
      context.operator.stop()
      context.operator.start()
    }

    private fun awaitReconcile(logsCaptor: LogCaptor) {
      await atMost
          5.seconds until
          {
            logsCaptor.hasInfoMessage("Starting reconciliation") &&
                logsCaptor.hasInfoMessage("Finished reconciling")
          }
    }

    @RegisterExtension val koinTestExtension = createKoinTestExtension()

    @RegisterExtension val kubernetesOperatorExtension = createKubernetesOperatorExtension()
  }
}
