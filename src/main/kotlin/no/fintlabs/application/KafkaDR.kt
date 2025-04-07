package no.fintlabs.application

import io.javaoperatorsdk.operator.api.config.informer.Informer
import io.javaoperatorsdk.operator.api.reconciler.Context
import io.javaoperatorsdk.operator.processing.dependent.kubernetes.CRUDKubernetesDependentResource
import io.javaoperatorsdk.operator.processing.dependent.kubernetes.KubernetesDependent
import no.fintlabs.application.api.MANAGED_BY_FLAISERATOR_SELECTOR
import no.fintlabs.application.api.v1alpha1.FlaisApplicationCrd
import no.fintlabs.operator.dependent.ReconcileCondition
import no.fintlabs.v1alpha1.KafkaUserAndAcl
import no.fintlabs.v1alpha1.KafkaUserAndAclSpec
import no.fintlabs.v1alpha1.kafkauserandaclspec.Acls

@KubernetesDependent(
    informer = Informer(labelSelector = MANAGED_BY_FLAISERATOR_SELECTOR)
)
class KafkaDR : CRUDKubernetesDependentResource<KafkaUserAndAcl, FlaisApplicationCrd>(KafkaUserAndAcl::class.java), ReconcileCondition<FlaisApplicationCrd> {
    override fun name(): String = "kafka"

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

    override fun shouldReconcile(primary: FlaisApplicationCrd, context: Context<FlaisApplicationCrd>): Boolean
        = primary.spec.kafka.acls.isNotEmpty() && primary.spec.kafka.enabled
}