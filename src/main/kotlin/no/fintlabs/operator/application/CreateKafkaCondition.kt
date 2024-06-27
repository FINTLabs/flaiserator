package no.fintlabs.operator.application

import io.javaoperatorsdk.operator.api.reconciler.Context
import io.javaoperatorsdk.operator.api.reconciler.dependent.DependentResource
import io.javaoperatorsdk.operator.processing.dependent.workflow.Condition
import no.fintlabs.operator.application.api.v1alpha1.FlaisApplicationCrd
import no.fintlabs.v1alpha1.KafkaUserAndAcl

class CreateKafkaCondition : Condition<KafkaUserAndAcl, FlaisApplicationCrd> {
    override fun isMet(
        dependentResource: DependentResource<KafkaUserAndAcl, FlaisApplicationCrd>?,
        primary: FlaisApplicationCrd,
        context: Context<FlaisApplicationCrd>
    ): Boolean = primary.spec.kafka.acls.isNotEmpty() && primary.spec.kafka.enabled
}