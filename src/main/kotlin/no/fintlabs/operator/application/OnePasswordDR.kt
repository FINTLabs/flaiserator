package no.fintlabs.operator.application

import com.onepassword.v1.OnePasswordItem
import io.javaoperatorsdk.operator.api.reconciler.Context
import io.javaoperatorsdk.operator.processing.dependent.kubernetes.CRUDKubernetesDependentResource
import io.javaoperatorsdk.operator.processing.dependent.kubernetes.KubernetesDependent
import no.fintlabs.operator.application.api.FlaisApplicationCrd

@KubernetesDependent
class OnePasswordDR : CRUDKubernetesDependentResource<OnePasswordItem, FlaisApplicationCrd>(OnePasswordItem::class.java) {
    override fun desired(primary: FlaisApplicationCrd, context: Context<FlaisApplicationCrd>) = OnePasswordItem().apply {
        metadata = createObjectMeta(primary)
        spec = primary.spec.onePassword
    }

    companion object {
        const val COMPONENT = "onepassword"
    }
}