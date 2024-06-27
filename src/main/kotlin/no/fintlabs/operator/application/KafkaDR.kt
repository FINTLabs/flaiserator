package no.fintlabs.operator.application

import io.javaoperatorsdk.operator.api.reconciler.Context
import io.javaoperatorsdk.operator.processing.dependent.kubernetes.CRUDKubernetesDependentResource
import io.javaoperatorsdk.operator.processing.dependent.kubernetes.KubernetesDependent
import no.fintlabs.operator.application.api.MANAGED_BY_FLAISERATOR_SELECTOR
import no.fintlabs.operator.application.api.v1alpha1.FlaisApplicationCrd
import no.fintlabs.v1alpha1.KafkaUserAndAcl
import no.fintlabs.v1alpha1.KafkaUserAndAclSpec
import no.fintlabs.v1alpha1.kafkauserandaclspec.Acls

@KubernetesDependent(
    labelSelector = MANAGED_BY_FLAISERATOR_SELECTOR
)
class KafkaDR : CRUDKubernetesDependentResource<KafkaUserAndAcl, FlaisApplicationCrd>(KafkaUserAndAcl::class.java) {
    override fun desired(primary: FlaisApplicationCrd, context: Context<FlaisApplicationCrd>) = KafkaUserAndAcl().apply {
        metadata = createObjectMeta(primary)
        spec = createKafkaUserAndAclSpec(primary)
    }

    private fun createKafkaUserAndAclSpec(primary: FlaisApplicationCrd) = KafkaUserAndAclSpec().apply {
        acls = primary.spec.kafka.acls.map { acl ->
            Acls().apply {
                topic = acl.topic
                permission = acl.permission
            }
        }
    }

    companion object {
        const val COMPONENT = "kafka"
    }
}