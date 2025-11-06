package no.fintlabs.common

import io.fabric8.kubernetes.api.model.ObjectMeta
import io.fabric8.kubernetes.api.model.OwnerReference
import io.javaoperatorsdk.operator.api.reconciler.BaseControl
import no.fintlabs.application.api.DEPLOYMENT_CORRELATION_ID_ANNOTATION
import no.fintlabs.application.api.MANAGED_BY_FLAISERATOR_LABEL
import no.fintlabs.common.api.v1alpha1.FlaisResource
import org.slf4j.Logger
import org.slf4j.LoggerFactory

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

fun createOwnerReference(source: FlaisResource<*>) =
    OwnerReference().apply {
      apiVersion = source.apiVersion
      kind = source.kind
      name = source.metadata.name
      uid = source.metadata.uid
      controller = true
    }

inline fun <reified T> T.getLogger(): Logger = LoggerFactory.getLogger(T::class.java)


fun <T : BaseControl<T>> BaseControl<T>.rescheduleImmediate(): T =
  rescheduleAfter(0L)