package no.fintlabs.operator

import io.fabric8.kubernetes.api.model.Service
import io.fabric8.kubernetes.api.model.ServicePort
import io.fabric8.kubernetes.api.model.ServiceSpec
import io.javaoperatorsdk.operator.api.config.informer.Informer
import io.javaoperatorsdk.operator.api.reconciler.Context
import io.javaoperatorsdk.operator.processing.dependent.kubernetes.CRUDKubernetesDependentResource
import io.javaoperatorsdk.operator.processing.dependent.kubernetes.KubernetesDependent
import no.fintlabs.operator.api.MANAGED_BY_FLAISERATOR_SELECTOR
import no.fintlabs.operator.api.v1alpha1.FlaisApplicationCrd

@KubernetesDependent(
    informer = Informer(labelSelector = MANAGED_BY_FLAISERATOR_SELECTOR)
)
class ServiceDR : CRUDKubernetesDependentResource<Service, FlaisApplicationCrd>(Service::class.java) {
    companion object {
        const val COMPONENT = "service"
    }

    override fun desired(primary: FlaisApplicationCrd, context: Context<FlaisApplicationCrd>): Service = Service().apply {
        metadata = createObjectMeta(primary)
        spec = ServiceSpec().apply {
            type = "ClusterIP"
            ports = listOf(
                ServicePort().apply {
                    name = "http"
                    protocol = "TCP"
                    port = primary.spec.port
                }
            )
            selector = mapOf("app" to primary.metadata.name)
        }
    }
}