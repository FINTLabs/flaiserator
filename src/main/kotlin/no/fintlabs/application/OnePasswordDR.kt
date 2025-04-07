package no.fintlabs.application

import com.onepassword.v1.OnePasswordItem
import com.onepassword.v1.OnePasswordItemSpec
import io.javaoperatorsdk.operator.api.config.informer.Informer
import io.javaoperatorsdk.operator.api.reconciler.Context
import io.javaoperatorsdk.operator.processing.dependent.kubernetes.CRUDKubernetesDependentResource
import io.javaoperatorsdk.operator.processing.dependent.kubernetes.KubernetesDependent
import no.fintlabs.application.api.MANAGED_BY_FLAISERATOR_SELECTOR
import no.fintlabs.application.api.v1alpha1.FlaisApplicationCrd
import no.fintlabs.operator.dependent.ReconcileCondition

@KubernetesDependent(
    informer = Informer(labelSelector = MANAGED_BY_FLAISERATOR_SELECTOR)
)
class OnePasswordDR : CRUDKubernetesDependentResource<OnePasswordItem, FlaisApplicationCrd>(OnePasswordItem::class.java), ReconcileCondition<FlaisApplicationCrd> {
    override fun name(): String = "onepassword"

    override fun desired(primary: FlaisApplicationCrd, context: Context<FlaisApplicationCrd>) = OnePasswordItem().apply {
        metadata = createObjectMeta(primary).apply {
            name = "${primary.metadata.name}-op"
        }
        spec = OnePasswordItemSpec().apply {
            itemPath = primary.spec.onePassword?.itemPath
        }
    }

    override fun shouldReconcile(primary: FlaisApplicationCrd, context: Context<FlaisApplicationCrd>): Boolean
        = primary.spec.onePassword != null && primary.spec.onePassword!!.itemPath.isNotEmpty()
}