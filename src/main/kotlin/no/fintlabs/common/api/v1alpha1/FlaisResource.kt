package no.fintlabs.common.api.v1alpha1

import io.fabric8.kubernetes.api.model.Namespaced
import io.fabric8.kubernetes.api.model.ObjectMeta
import io.fabric8.kubernetes.client.CustomResource

abstract class FlaisResource<T> : CustomResource<T, FlaisResourceStatus>(), Namespaced

fun <T : FlaisResource<*>> T.clone() : T {
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