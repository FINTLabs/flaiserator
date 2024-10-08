package no.fintlabs.operator

import io.javaoperatorsdk.operator.api.reconciler.Context
import io.javaoperatorsdk.operator.api.reconciler.dependent.DependentResource
import io.javaoperatorsdk.operator.processing.dependent.workflow.Condition
import no.fintlabs.operator.api.v1alpha1.FlaisApplicationCrd
import no.fintlabs.operator.api.v1alpha1.isIngressEnabled
import us.containo.traefik.v1alpha1.IngressRoute

class CreateIngressCondition : Condition<IngressRoute, FlaisApplicationCrd>{
    override fun isMet(
        dependentResource: DependentResource<IngressRoute, FlaisApplicationCrd>,
        primary: FlaisApplicationCrd,
        context: Context<FlaisApplicationCrd>
    ) = primary.spec.isIngressEnabled()
}