package no.fintlabs.common

import io.fabric8.kubernetes.api.model.EnvFromSource
import io.fabric8.kubernetes.api.model.SecretEnvSource
import io.javaoperatorsdk.operator.api.config.informer.Informer
import io.javaoperatorsdk.operator.api.reconciler.Context
import io.javaoperatorsdk.operator.processing.dependent.kubernetes.CRUDKubernetesDependentResource
import io.javaoperatorsdk.operator.processing.dependent.kubernetes.KubernetesDependent
import no.fintlabs.application.api.MANAGED_BY_FLAISERATOR_SELECTOR
import no.fintlabs.common.api.v1alpha1.Database
import no.fintlabs.common.api.v1alpha1.FlaisResource
import no.fintlabs.common.api.v1alpha1.FlaisResourceSpec
import no.fintlabs.common.pod.PodBuilderContext
import no.fintlabs.common.pod.PodCustomizer
import no.fintlabs.operator.dependent.ReconcileCondition
import no.fintlabs.v1alpha1.PGUser
import no.fintlabs.v1alpha1.PGUserSpec

interface WithPostgres : FlaisResourceSpec {
  val database: Database
}

@KubernetesDependent(informer = Informer(labelSelector = MANAGED_BY_FLAISERATOR_SELECTOR))
class PostgresUserDR<P : FlaisResource<out WithPostgres>> :
    CRUDKubernetesDependentResource<PGUser, P>(PGUser::class.java),
    ReconcileCondition<P>,
    PodCustomizer<P> {
  override fun name(): String = "postgres-user"

  override fun desired(
      primary: P,
      context: Context<P>,
  ): PGUser =
      PGUser().apply {
        metadata = createObjectMeta(primary).apply { name = "${primary.metadata.name}-db" }
        spec = PGUserSpec().apply { database = primary.spec.database.database!! }
      }

  override fun shouldReconcile(
      primary: P,
      context: Context<P>,
  ): Boolean = !primary.spec.database.database.isNullOrEmpty()

  override fun customizePod(primary: P, builderContext: PodBuilderContext, context: Context<P>) {
    if (!shouldReconcile(primary, context)) return

    builderContext.envFrom +=
        EnvFromSource().apply {
          secretRef = SecretEnvSource().apply { name = "${primary.metadata.name}-db" }
        }
  }
}
