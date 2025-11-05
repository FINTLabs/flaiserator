package no.fintlabs.common

import io.javaoperatorsdk.operator.api.config.informer.Informer
import io.javaoperatorsdk.operator.api.reconciler.Context
import io.javaoperatorsdk.operator.processing.dependent.kubernetes.CRUDKubernetesDependentResource
import io.javaoperatorsdk.operator.processing.dependent.kubernetes.KubernetesDependent
import no.fintlabs.application.api.MANAGED_BY_FLAISERATOR_SELECTOR
import no.fintlabs.common.api.v1alpha1.Kafka
import no.fintlabs.application.createObjectMeta
import no.fintlabs.common.api.v1alpha1.FlaisResource
import no.fintlabs.operator.dependent.ReconcileCondition
import no.fintlabs.v1alpha1.KafkaUserAndAcl
import no.fintlabs.v1alpha1.KafkaUserAndAclSpec
import no.fintlabs.v1alpha1.kafkauserandaclspec.Acls

interface WithKafka {
  val kafka: Kafka
}

@KubernetesDependent(informer = Informer(labelSelector = MANAGED_BY_FLAISERATOR_SELECTOR))
class KafkaDR<P : FlaisResource<out WithKafka>> :
  CRUDKubernetesDependentResource<KafkaUserAndAcl, P>(KafkaUserAndAcl::class.java),
  ReconcileCondition<P> {
  override fun name(): String = "kafka"

  override fun desired(primary: P, context: Context<P>) =
      KafkaUserAndAcl().apply {
        metadata = createObjectMeta(primary)
        spec = createKafkaUserAndAclSpec(primary)
      }

  private fun createKafkaUserAndAclSpec(primary: P) =
      KafkaUserAndAclSpec().apply {
        acls =
            primary.spec.kafka.acls.map { acl ->
              Acls().apply {
                topic = acl.topic
                permission = acl.permission
              }
            }
      }

  override fun shouldReconcile(
    primary: P,
    context: Context<P>,
  ): Boolean = primary.spec.kafka.acls.isNotEmpty() && primary.spec.kafka.enabled
}
