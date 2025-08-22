package no.fintlabs.application

import io.javaoperatorsdk.operator.api.config.informer.Informer
import io.javaoperatorsdk.operator.api.reconciler.Context
import io.javaoperatorsdk.operator.processing.dependent.kubernetes.CRUDKubernetesDependentResource
import io.javaoperatorsdk.operator.processing.dependent.kubernetes.KubernetesDependent
import no.fintlabs.application.api.MANAGED_BY_FLAISERATOR_SELECTOR
import no.fintlabs.application.api.v1alpha1.FlaisApplicationCrd
import no.fintlabs.operator.dependent.ReconcileCondition
import no.fintlabs.v1alpha1.PGUser
import no.fintlabs.v1alpha1.PGUserSpec

@KubernetesDependent(informer = Informer(labelSelector = MANAGED_BY_FLAISERATOR_SELECTOR))
class PostgresUserDR :
    CRUDKubernetesDependentResource<PGUser, FlaisApplicationCrd>(PGUser::class.java),
    ReconcileCondition<FlaisApplicationCrd> {
  override fun name(): String = "postgres-user"

  override fun desired(
      primary: FlaisApplicationCrd,
      context: Context<FlaisApplicationCrd>
  ): PGUser =
      PGUser().apply {
        metadata = createObjectMeta(primary).apply { name = "${primary.metadata.name}-db" }
        spec = PGUserSpec().apply { database = primary.spec.database.database!! }
      }

  override fun shouldReconcile(
      primary: FlaisApplicationCrd,
      context: Context<FlaisApplicationCrd>
  ): Boolean = !primary.spec.database.database.isNullOrEmpty()
}
