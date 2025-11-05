package no.fintlabs.application

import io.fabric8.kubernetes.api.model.Service
import io.fabric8.kubernetes.api.model.ServicePort
import io.fabric8.kubernetes.api.model.ServiceSpec
import io.javaoperatorsdk.operator.api.config.informer.Informer
import io.javaoperatorsdk.operator.api.reconciler.Context
import io.javaoperatorsdk.operator.processing.dependent.kubernetes.CRUDKubernetesDependentResource
import io.javaoperatorsdk.operator.processing.dependent.kubernetes.KubernetesDependent
import no.fintlabs.application.api.MANAGED_BY_FLAISERATOR_SELECTOR
import no.fintlabs.application.api.v1alpha1.FlaisApplication

@KubernetesDependent(informer = Informer(labelSelector = MANAGED_BY_FLAISERATOR_SELECTOR))
class ServiceDR :
    CRUDKubernetesDependentResource<Service, FlaisApplication>(Service::class.java) {
  override fun name(): String = "service"

  override fun desired(
      primary: FlaisApplication,
      context: Context<FlaisApplication>,
  ): Service =
      Service().apply {
        metadata = createObjectMeta(primary)
        spec =
            ServiceSpec().apply {
              type = "ClusterIP"
              ports =
                  listOf(
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
