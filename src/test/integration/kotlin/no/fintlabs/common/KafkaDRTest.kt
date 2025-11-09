package no.fintlabs.common

import no.fintlabs.common.Utils.createAndGetResource
import no.fintlabs.common.Utils.createKoinTestExtension
import no.fintlabs.common.Utils.createKubernetesOperatorExtension
import no.fintlabs.common.Utils.createTestResource
import no.fintlabs.common.api.v1alpha1.Kafka
import no.fintlabs.extensions.KubernetesOperatorContext
import no.fintlabs.v1alpha1.KafkaUserAndAcl
import no.fintlabs.v1alpha1.kafkauserandaclspec.Acls
import org.junit.jupiter.api.extension.RegisterExtension
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class KafkaDRTest {
  // region General
  @Test
  fun `should create KafkaUserAndAcl`(context: KubernetesOperatorContext) {
    val testResource =
      createTestResource().apply {
          spec =
              spec.copy(
                  kafka =
                      Kafka(
                          true,
                          listOf(
                              Acls().apply {
                                topic = "test-topic"
                                permission = "write"
                              }
                          ),
                      )
              )
        }

    val kafkaUserAndAcl = context.createAndKafkaUserAndAcl(testResource)
    assertNotNull(kafkaUserAndAcl)
    assertEquals(testResource.metadata.name, kafkaUserAndAcl.metadata.name)
    assertEquals("test-topic", kafkaUserAndAcl.spec.acls[0].topic)
    assertEquals("write", kafkaUserAndAcl.spec.acls[0].permission)
  }

  @Test
  fun `should create KafkaUserAndAcl with multiple acls`(context: KubernetesOperatorContext) {
    val testResource =
      createTestResource().apply {
          spec =
              spec.copy(
                  kafka =
                      Kafka(
                          true,
                          listOf(
                              Acls().apply {
                                topic = "test-topic"
                                permission = "write"
                              },
                              Acls().apply {
                                topic = "test-topic-2"
                                permission = "read"
                              },
                          ),
                      )
              )
        }

    val kafkaUserAndAcl = context.createAndKafkaUserAndAcl(testResource)
    assertNotNull(kafkaUserAndAcl)
    assertEquals(2, kafkaUserAndAcl.spec.acls.size)
    assertEquals("test-topic", kafkaUserAndAcl.spec.acls[0].topic)
    assertEquals("write", kafkaUserAndAcl.spec.acls[0].permission)
    assertEquals("test-topic-2", kafkaUserAndAcl.spec.acls[1].topic)
    assertEquals("read", kafkaUserAndAcl.spec.acls[1].permission)
  }

  @Test
  fun `should not create KafkaUserAndAcl since enabled is false`(
      context: KubernetesOperatorContext
  ) {
    val testResource =
      createTestResource().apply { spec = spec.copy(kafka = Kafka(false)) }

    val kafkaUserAndAcl = context.createAndKafkaUserAndAcl(testResource)
    assertNull(kafkaUserAndAcl)
  }

  @Test
  fun `should not create KafkaUserAndAcl since acls is not set`(
      context: KubernetesOperatorContext
  ) {
    val testResource =
      createTestResource().apply { spec = spec.copy(kafka = Kafka(true)) }

    val kafkaUserAndAcl = context.createAndKafkaUserAndAcl(testResource)
    assertNull(kafkaUserAndAcl)
  }

  // endregion

  private fun KubernetesOperatorContext.createAndKafkaUserAndAcl(resource: FlaisTestResource) =
      createAndGetResource<KafkaUserAndAcl>(resource)

  companion object {
    @RegisterExtension val koinTestExtension = createKoinTestExtension()

    @RegisterExtension val kubernetesOperatorExtension = createKubernetesOperatorExtension()
  }
}
