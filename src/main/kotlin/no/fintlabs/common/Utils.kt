package no.fintlabs.common

import io.fabric8.kubernetes.api.model.HasMetadata
import io.fabric8.kubernetes.api.model.ObjectMeta
import io.fabric8.kubernetes.api.model.OwnerReference
import io.javaoperatorsdk.operator.api.reconciler.BaseControl
import io.javaoperatorsdk.operator.api.reconciler.Context
import no.fintlabs.application.api.DEPLOYMENT_CORRELATION_ID_ANNOTATION
import no.fintlabs.application.api.MANAGED_BY_FLAISERATOR_LABEL
import no.fintlabs.common.api.v1alpha1.FlaisResource
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import kotlin.jvm.java
import kotlin.jvm.optionals.getOrNull

fun createObjectMeta(source: FlaisResource<*>) =
    ObjectMeta().apply {
      name = source.metadata.name
      namespace = source.metadata.namespace

      labels =
          source.metadata.labels
              .toMutableMap()
              .plus("app" to source.metadata.name)
              .plus(MANAGED_BY_FLAISERATOR_LABEL)
      annotations =
          mutableMapOf(
              DEPLOYMENT_CORRELATION_ID_ANNOTATION to
                  source.metadata.annotations[DEPLOYMENT_CORRELATION_ID_ANNOTATION]
          )
      ownerReferences = listOf(createOwnerReference(source))
    }

fun createOwnerReference(source: HasMetadata) =
    OwnerReference().apply {
      apiVersion = source.apiVersion
      kind = source.kind
      name = source.metadata.name
      uid = source.metadata.uid
    }

inline fun <reified T> T.getLogger(): Logger = LoggerFactory.getLogger(T::class.java)


fun <T : BaseControl<T>> BaseControl<T>.rescheduleImmediate(): T =
  rescheduleAfter(0L)

inline fun <reified P: HasMetadata> Context<*>.getSecondaryResource(): P? =
  this.getSecondaryResource<P>(P::class.java).getOrNull()

inline fun <reified P: HasMetadata> Context<*>.getRequiredSecondaryResource(): P =
  getSecondaryResource<P>().let {
    if (it == null) error("Required resource is null")
    it
  }