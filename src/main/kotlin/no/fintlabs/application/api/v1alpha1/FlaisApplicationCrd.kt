package no.fintlabs.application.api.v1alpha1

import io.fabric8.kubernetes.api.model.Namespaced
import io.fabric8.kubernetes.api.model.ObjectMeta
import io.fabric8.kubernetes.client.CustomResource
import io.fabric8.kubernetes.model.annotation.Group
import io.fabric8.kubernetes.model.annotation.Kind
import io.fabric8.kubernetes.model.annotation.Version

@Group("fintlabs.no")
@Version("v1alpha1")
@Kind("Application")
class FlaisApplicationCrd :
    CustomResource<FlaisApplicationSpec, FlaisApplicationStatus>(), Namespaced {}

fun FlaisApplicationCrd.clone() =
    FlaisApplicationCrd().apply {
      metadata =
          ObjectMeta().apply {
            name = this@clone.metadata.name
            namespace = this@clone.metadata.namespace
            managedFields = null
          }
    }
