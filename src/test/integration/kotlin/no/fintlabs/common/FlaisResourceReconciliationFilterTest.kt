package no.fintlabs.common

import nl.altindag.log.LogCaptor
import no.fintlabs.common.Utils.createAndGetResource
import no.fintlabs.common.Utils.createKoinTestExtension
import no.fintlabs.common.Utils.createKubernetesOperatorExtension
import no.fintlabs.common.Utils.createTestResource
import no.fintlabs.extensions.KubernetesOperatorContext
import org.awaitility.kotlin.atMost
import org.awaitility.kotlin.await
import org.awaitility.kotlin.until
import org.junit.jupiter.api.extension.RegisterExtension
import org.koin.test.KoinTest
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.time.Duration.Companion.seconds


class FlaisResourceReconciliationFilterTest : KoinTest {
  @Test
  fun `should not reconcile non updated resources on startup`(context: KubernetesOperatorContext) {
    context.createAndGetTestResource(createTestResource())
    context.operator.stop()
    val logsCaptor = LogCaptor.forName("io.javaoperatorsdk").apply {
      setLogLevelToDebug()
    }
    context.operator.start()
    await atMost 5.seconds until { logsCaptor.hasDebugMessage("Skipping event handling resource") }
  }

  @Test
  fun `should reconcile updated resources on startup`(context: KubernetesOperatorContext) {
    val resource = context.createAndGetTestResource(createTestResource())
    assertNotNull(resource)
    context.operator.stop()
    val logsCaptor = LogCaptor.forName("no.fintlabs.common.FlaisResourceReconciler").apply {
      setLogLevelToDebug()
    }
    context.update(resource.apply {
      spec = spec.copy(image = "123")
    })
    context.operator.start()
    await atMost 5.seconds until {
      logsCaptor.hasInfoMessage("Starting reconciliation") &&
      logsCaptor.hasInfoMessage("Finished reconciling")
    }
  }

  companion object {
    private fun KubernetesOperatorContext.createAndGetTestResource(resource: FlaisTestResource) =
      createAndGetResource<FlaisTestResource>(resource)

    @RegisterExtension val koinTestExtension = createKoinTestExtension()

    @RegisterExtension val kubernetesOperatorExtension = createKubernetesOperatorExtension()
  }
}
