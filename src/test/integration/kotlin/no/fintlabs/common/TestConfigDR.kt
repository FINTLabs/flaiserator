package no.fintlabs.common

import io.fabric8.kubernetes.api.model.ConfigMap
import io.javaoperatorsdk.operator.api.reconciler.Context
import io.javaoperatorsdk.operator.processing.dependent.Creator
import io.javaoperatorsdk.operator.processing.dependent.kubernetes.KubernetesDependent
import io.javaoperatorsdk.operator.processing.dependent.kubernetes.KubernetesDependentResource

@KubernetesDependent
class TestConfigDR :
    KubernetesDependentResource<ConfigMap, FlaisTestResource>(ConfigMap::class.java),
    Creator<ConfigMap, FlaisTestResource> {
  override fun desired(primary: FlaisTestResource, context: Context<FlaisTestResource>): ConfigMap =
      ConfigMap().apply {
        metadata = createObjectMeta(primary)
        data = mapOf("name" to primary.metadata.name)
      }
}
