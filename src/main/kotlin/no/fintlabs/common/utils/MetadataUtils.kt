package no.fintlabs.common.utils

import io.fabric8.kubernetes.api.model.HasMetadata
import io.fabric8.kubernetes.api.model.ObjectMeta
import io.fabric8.kubernetes.api.model.OwnerReference
import no.fintlabs.application.api.DEPLOYMENT_CORRELATION_ID_ANNOTATION
import no.fintlabs.application.api.MANAGED_BY_FLAISERATOR_LABEL
import no.fintlabs.common.api.v1alpha1.FlaisResource

fun createObjectMeta(source: FlaisResource<*>) =
    ObjectMeta().apply {
      name = source.metadata.name
      namespace = source.metadata.namespace

      labels =
          source.metadata.labels
              .toMutableMap()
              .plus("app" to source.metadata.name)
              .plus(MANAGED_BY_FLAISERATOR_LABEL)

      correlationIdAnnotation = source.metadata.correlationIdAnnotation
      ownerReferences = listOf(createOwnerReference(source))
    }

fun createOwnerReference(source: HasMetadata) =
    OwnerReference().apply {
      apiVersion = source.apiVersion
      kind = source.kind
      name = source.metadata.name
      uid = source.metadata.uid
    }

var ObjectMeta.correlationIdAnnotation: String?
  get() = this.annotations?.get(DEPLOYMENT_CORRELATION_ID_ANNOTATION)
  set(value) {
    if (this.annotations == null) {
      this.annotations = mutableMapOf(DEPLOYMENT_CORRELATION_ID_ANNOTATION to value)
    } else {
      this.annotations[DEPLOYMENT_CORRELATION_ID_ANNOTATION] = value
    }
  }
