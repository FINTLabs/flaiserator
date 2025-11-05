package no.fintlabs.common

import com.onepassword.v1.OnePasswordItem
import com.onepassword.v1.OnePasswordItemSpec
import io.javaoperatorsdk.operator.api.config.informer.Informer
import io.javaoperatorsdk.operator.api.reconciler.Context
import io.javaoperatorsdk.operator.processing.dependent.kubernetes.CRUDKubernetesDependentResource
import io.javaoperatorsdk.operator.processing.dependent.kubernetes.KubernetesDependent
import no.fintlabs.application.api.MANAGED_BY_FLAISERATOR_SELECTOR
import no.fintlabs.application.createObjectMeta
import no.fintlabs.common.api.v1alpha1.FlaisResource
import no.fintlabs.common.api.v1alpha1.OnePassword
import no.fintlabs.operator.dependent.ReconcileCondition

interface WithOnePassword {
  val onePassword: OnePassword?
}

@KubernetesDependent(informer = Informer(labelSelector = MANAGED_BY_FLAISERATOR_SELECTOR))
class OnePasswordDR<P : FlaisResource<out WithOnePassword>> :
    CRUDKubernetesDependentResource<OnePasswordItem, P>(OnePasswordItem::class.java),
    ReconcileCondition<P> {
  override fun name(): String = "onepassword"

  override fun desired(primary: P, context: Context<P>) =
      OnePasswordItem().apply {
        metadata = createObjectMeta(primary).apply { name = "${primary.metadata.name}-op" }
        spec = OnePasswordItemSpec().apply { itemPath = primary.spec.onePassword?.itemPath }
      }

  override fun shouldReconcile(
      primary: P,
      context: Context<P>,
  ): Boolean = primary.spec.onePassword != null && primary.spec.onePassword!!.itemPath.isNotEmpty()
}
