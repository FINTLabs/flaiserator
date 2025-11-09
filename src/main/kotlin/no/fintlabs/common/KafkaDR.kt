package no.fintlabs.common

import io.fabric8.kubernetes.api.model.EnvFromSource
import io.fabric8.kubernetes.api.model.SecretEnvSource
import io.fabric8.kubernetes.api.model.SecretVolumeSource
import io.fabric8.kubernetes.api.model.Volume
import io.fabric8.kubernetes.api.model.VolumeMount
import io.javaoperatorsdk.operator.api.config.informer.Informer
import io.javaoperatorsdk.operator.api.reconciler.Context
import io.javaoperatorsdk.operator.processing.dependent.kubernetes.CRUDKubernetesDependentResource
import io.javaoperatorsdk.operator.processing.dependent.kubernetes.KubernetesDependent
import no.fintlabs.application.api.MANAGED_BY_FLAISERATOR_SELECTOR
import no.fintlabs.common.api.v1alpha1.FlaisResource
import no.fintlabs.common.api.v1alpha1.FlaisResourceSpec
import no.fintlabs.common.api.v1alpha1.Kafka
import no.fintlabs.common.pod.PodBuilderContext
import no.fintlabs.common.pod.PodCustomizer
import no.fintlabs.operator.dependent.ReconcileCondition
import no.fintlabs.v1alpha1.KafkaUserAndAcl
import no.fintlabs.v1alpha1.KafkaUserAndAclSpec
import no.fintlabs.v1alpha1.kafkauserandaclspec.Acls

interface WithKafka : FlaisResourceSpec {
  val kafka: Kafka
}

@KubernetesDependent(informer = Informer(labelSelector = MANAGED_BY_FLAISERATOR_SELECTOR))
class KafkaDR<P : FlaisResource<out WithKafka>> :
    CRUDKubernetesDependentResource<KafkaUserAndAcl, P>(KafkaUserAndAcl::class.java),
    ReconcileCondition<P>,
    PodCustomizer<P> {
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

  override fun customizePod(primary: P, builderContext: PodBuilderContext, context: Context<P>) {
    if (!shouldReconcile(primary, context)) return

    builderContext.volumes +=
        Volume().apply {
          name = "credentials"
          secret =
              SecretVolumeSource().apply {
                secretName = "${primary.metadata.name}-kafka-certificates"
              }
        }

    builderContext.volumeMounts +=
        VolumeMount().apply {
          name = "credentials"
          mountPath = "/credentials"
          readOnly = true
        }

    builderContext.envFrom +=
        EnvFromSource().apply {
          secretRef = SecretEnvSource().apply { name = "${primary.metadata.name}-kafka" }
        }
  }
}
