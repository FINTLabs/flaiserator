package no.fintlabs.operator.application

import io.javaoperatorsdk.operator.api.reconciler.Context
import io.javaoperatorsdk.operator.processing.dependent.kubernetes.CRUDKubernetesDependentResource
import io.javaoperatorsdk.operator.processing.dependent.kubernetes.KubernetesDependent
import no.fintlabs.operator.application.api.FlaisApplicationCrd
import no.fintlabs.operator.application.api.MANAGED_BY_FLAISERATOR_SELECTOR
import no.fintlabs.v1alpha1.PGUser
import no.fintlabs.v1alpha1.PGUserSpec

@KubernetesDependent(
    labelSelector = MANAGED_BY_FLAISERATOR_SELECTOR
)
class PostgresUserDR : CRUDKubernetesDependentResource<PGUser, FlaisApplicationCrd>(PGUser::class.java) {
    companion object {
        const val COMPONENT = "postgres-user"
    }

    override fun desired(primary: FlaisApplicationCrd, context: Context<FlaisApplicationCrd>): PGUser = PGUser().apply {
        metadata = createObjectMeta(primary).apply {
            name = "${primary.metadata.name}-db"
        }
        spec = PGUserSpec().apply {
            database = primary.spec.database.database!!
        }
    }
}