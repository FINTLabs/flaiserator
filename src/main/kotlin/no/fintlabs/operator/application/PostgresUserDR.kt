package no.fintlabs.operator.application

import io.javaoperatorsdk.operator.api.reconciler.Context
import io.javaoperatorsdk.operator.processing.dependent.kubernetes.CRUDKubernetesDependentResource
import no.fintlabs.operator.application.api.FlaisApplicationCrd
import no.fintlabs.v1alpha1.PGUser
import no.fintlabs.v1alpha1.PGUserSpec

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