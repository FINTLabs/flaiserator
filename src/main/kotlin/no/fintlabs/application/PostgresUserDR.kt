package no.fintlabs.application

import io.javaoperatorsdk.operator.api.config.informer.Informer
import io.javaoperatorsdk.operator.api.reconciler.Context
import io.javaoperatorsdk.operator.processing.dependent.kubernetes.CRUDKubernetesDependentResource
import io.javaoperatorsdk.operator.processing.dependent.kubernetes.KubernetesDependent
import no.fintlabs.application.api.MANAGED_BY_FLAISERATOR_SELECTOR
import no.fintlabs.application.api.v1alpha1.FlaisApplication
import no.fintlabs.common.createObjectMeta
import no.fintlabs.operator.dependent.ReconcileCondition
import no.fintlabs.v1alpha1.PGUser
import no.fintlabs.v1alpha1.PGUserSpec

@KubernetesDependent(informer = Informer(labelSelector = MANAGED_BY_FLAISERATOR_SELECTOR))
class PostgresUserDR :
    CRUDKubernetesDependentResource<PGUser, FlaisApplication>(PGUser::class.java),
    ReconcileCondition<FlaisApplication> {
  override fun name(): String = "postgres-user"

  override fun desired(
      primary: FlaisApplication,
      context: Context<FlaisApplication>,
  ): PGUser =
      PGUser().apply {
        metadata = createObjectMeta(primary).apply { name = "${primary.metadata.name}-db" }
        spec = PGUserSpec().apply { database = primary.spec.database.database!! }
      }

  override fun shouldReconcile(
      primary: FlaisApplication,
      context: Context<FlaisApplication>,
  ): Boolean = !primary.spec.database.database.isNullOrEmpty()
}
