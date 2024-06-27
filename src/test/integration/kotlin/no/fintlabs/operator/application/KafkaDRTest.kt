package no.fintlabs.operator.application

import no.fintlabs.extensions.KubernetesOperatorContext
import no.fintlabs.operator.application.Utils.createAndGetResource
import no.fintlabs.operator.application.Utils.createKoinTestExtension
import no.fintlabs.operator.application.Utils.createKubernetesOperatorExtension
import no.fintlabs.operator.application.Utils.createTestFlaisApplication
import no.fintlabs.operator.application.api.v1alpha1.FlaisApplicationCrd
import no.fintlabs.operator.application.api.v1alpha1.Kafka
import no.fintlabs.v1alpha1.KafkaUserAndAcl
import no.fintlabs.v1alpha1.kafkauserandaclspec.Acls
import org.junit.jupiter.api.extension.RegisterExtension
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class KafkaDRTest {
    //region General
    @Test
    fun `should create KafkaUserAndAcl`(context: KubernetesOperatorContext) {
        val flaisApplication = createTestFlaisApplication().apply {
            spec = spec.copy(kafka = Kafka(true, listOf(Acls().apply {
                topic = "test-topic"
                permission = "write"
            })))
        }

        val kafkaUserAndAcl = context.createAndKafkaUserAndAcl(flaisApplication)
        assertNotNull(kafkaUserAndAcl)
        assertEquals(flaisApplication.metadata.name, kafkaUserAndAcl.metadata.name)
        assertEquals("test-topic", kafkaUserAndAcl.spec.acls[0].topic)
        assertEquals("write", kafkaUserAndAcl.spec.acls[0].permission)
    }

    @Test
    fun `should create KafkaUserAndAcl with multiple acls`(context: KubernetesOperatorContext) {
        val flaisApplication = createTestFlaisApplication().apply {
            spec = spec.copy(kafka = Kafka(true, listOf(
                Acls().apply {
                    topic = "test-topic"
                    permission = "write"
                },
                Acls().apply {
                    topic = "test-topic-2"
                    permission = "read"
                }
            )))
        }

        val kafkaUserAndAcl = context.createAndKafkaUserAndAcl(flaisApplication)
        assertNotNull(kafkaUserAndAcl)
        assertEquals(2, kafkaUserAndAcl.spec.acls.size)
        assertEquals("test-topic", kafkaUserAndAcl.spec.acls[0].topic)
        assertEquals("write", kafkaUserAndAcl.spec.acls[0].permission)
        assertEquals("test-topic-2", kafkaUserAndAcl.spec.acls[1].topic)
        assertEquals("read", kafkaUserAndAcl.spec.acls[1].permission)
    }

    @Test
    fun `should not create KafkaUserAndAcl since enabled is false`(context: KubernetesOperatorContext) {
        val flaisApplication = createTestFlaisApplication().apply {
            spec = spec.copy(kafka = Kafka(false))
        }

        val kafkaUserAndAcl = context.createAndKafkaUserAndAcl(flaisApplication)
        assertNull(kafkaUserAndAcl)
    }

    @Test
    fun `should not create KafkaUserAndAcl since acls is not set`(context: KubernetesOperatorContext) {
        val flaisApplication = createTestFlaisApplication().apply {
            spec = spec.copy(kafka = Kafka(true))
        }

        val kafkaUserAndAcl = context.createAndKafkaUserAndAcl(flaisApplication)
        assertNull(kafkaUserAndAcl)
    }

    //endregion

    private fun KubernetesOperatorContext.createAndKafkaUserAndAcl(app: FlaisApplicationCrd) =
        createAndGetResource<KafkaUserAndAcl>(app)

    companion object {
        @RegisterExtension
        val koinTestExtension = createKoinTestExtension()

        @RegisterExtension
        val kubernetesOperatorExtension = createKubernetesOperatorExtension()
    }
}