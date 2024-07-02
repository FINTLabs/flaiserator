package no.fintlabs.operator

import com.onepassword.v1.OnePasswordItem
import com.onepassword.v1.OnePasswordItemSpec
import io.javaoperatorsdk.operator.api.reconciler.Context
import io.javaoperatorsdk.operator.processing.dependent.kubernetes.CRUDKubernetesDependentResource
import io.javaoperatorsdk.operator.processing.dependent.kubernetes.KubernetesDependent
import no.fintlabs.operator.api.MANAGED_BY_FLAISERATOR_SELECTOR
import no.fintlabs.operator.api.v1alpha1.FlaisApplicationCrd

@KubernetesDependent(
    labelSelector = MANAGED_BY_FLAISERATOR_SELECTOR
)
class OnePasswordDR : CRUDKubernetesDependentResource<OnePasswordItem, FlaisApplicationCrd>(OnePasswordItem::class.java) {
    override fun desired(primary: FlaisApplicationCrd, context: Context<FlaisApplicationCrd>) = OnePasswordItem().apply {
        metadata = createObjectMeta(primary).apply {
            name = "${primary.metadata.name}-op"
        }
        spec = OnePasswordItemSpec().apply {
            itemPath = primary.spec.onePassword?.itemPath
        }
    }

    companion object {
        const val COMPONENT = "onepassword"
    }
}