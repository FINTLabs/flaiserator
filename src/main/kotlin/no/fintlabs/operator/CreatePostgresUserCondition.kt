package no.fintlabs.operator

import io.javaoperatorsdk.operator.api.reconciler.Context
import io.javaoperatorsdk.operator.api.reconciler.dependent.DependentResource
import io.javaoperatorsdk.operator.processing.dependent.workflow.Condition
import no.fintlabs.operator.api.v1alpha1.FlaisApplicationCrd
import no.fintlabs.v1alpha1.PGUser

class CreatePostgresUserCondition : Condition<PGUser, FlaisApplicationCrd> {
    override fun isMet(
        dependentResource: DependentResource<PGUser, FlaisApplicationCrd>?,
        primary: FlaisApplicationCrd,
        context: Context<FlaisApplicationCrd>
    ) = !primary.spec.database.database.isNullOrEmpty()
}