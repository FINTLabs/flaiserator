package no.fintlabs.operator.application

import io.fabric8.kubernetes.api.model.Service
import io.fabric8.kubernetes.api.model.ServicePort
import io.fabric8.kubernetes.api.model.ServiceSpec
import io.javaoperatorsdk.operator.api.reconciler.Context
import io.javaoperatorsdk.operator.processing.dependent.kubernetes.CRUDKubernetesDependentResource
import no.fintlabs.operator.application.api.FlaisApplicationCrd

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
                    protocol = "TCP"
                    port = primary.spec.port
                }
            )
            selector = mapOf("app" to primary.metadata.name)
        }
    }
}