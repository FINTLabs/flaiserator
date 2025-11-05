package no.fintlabs.application.api.v1alpha1

import io.fabric8.kubernetes.api.model.ObjectMeta
import io.fabric8.kubernetes.model.annotation.Group
import io.fabric8.kubernetes.model.annotation.Kind
import io.fabric8.kubernetes.model.annotation.Version
import no.fintlabs.common.api.v1alpha1.FlaisResource

@Group("fintlabs.no")
@Version("v1alpha1")
@Kind("Application")
class FlaisApplication : FlaisResource<FlaisApplicationSpec>()

fun FlaisApplication.clone() =
  FlaisApplication().apply {
      metadata =
          ObjectMeta().apply {
            name = this@clone.metadata.name
            namespace = this@clone.metadata.namespace
            managedFields = null
          }
    }
