package no.fintlabs.operator

import com.onepassword.v1.OnePasswordItem
import io.javaoperatorsdk.operator.api.reconciler.Context
import io.javaoperatorsdk.operator.api.reconciler.dependent.DependentResource
import io.javaoperatorsdk.operator.processing.dependent.workflow.Condition
import no.fintlabs.operator.api.v1alpha1.FlaisApplicationCrd

class CreateOnePasswordCondition : Condition<OnePasswordItem, FlaisApplicationCrd> {
    override fun isMet(
        dependentResource: DependentResource<OnePasswordItem, FlaisApplicationCrd>?,
        primary: FlaisApplicationCrd,
        context: Context<FlaisApplicationCrd>
    ) = primary.spec.onePassword != null && primary.spec.onePassword!!.itemPath.isNotEmpty()
}