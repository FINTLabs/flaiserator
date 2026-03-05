package no.fintlabs.common.api.v1alpha1

import io.fabric8.kubernetes.api.model.Namespaced
import io.fabric8.kubernetes.api.model.ObjectMeta
import io.fabric8.kubernetes.client.CustomResource
import no.fintlabs.common.utils.createHash

abstract class FlaisResource<T : FlaisResourceSpec> :
    CustomResource<T, FlaisResourceStatus>(), Namespaced

fun <T : FlaisResource<*>> T.clone(): T {
  val resource = this.javaClass.getConstructor().newInstance()
  return resource.apply {
    metadata =
        ObjectMeta().apply {
          name = this@clone.metadata.name
          namespace = this@clone.metadata.namespace
          managedFields = null
        }
  }
}

fun <T : FlaisResource<*>> T.resourceHash(): String {
  val values =
      mapOf(
          "spec" to spec,
          "labels" to metadata.labels,
          "changeCause" to metadata.annotations?.get("kubernetes.io/change-cause"),
      )
  return createHash(values)
}
