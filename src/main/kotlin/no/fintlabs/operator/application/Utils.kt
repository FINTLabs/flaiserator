package no.fintlabs.operator.application

import io.fabric8.kubernetes.api.model.ObjectMeta
import io.fabric8.kubernetes.api.model.OwnerReference
import no.fintlabs.operator.application.api.DEPLOYMENT_CORRELATION_ID_ANNOTATION
import no.fintlabs.operator.application.api.FlaisApplicationCrd
import no.fintlabs.operator.application.api.MANAGED_BY_FLAISERATOR_LABEL

fun createObjectMeta(source: FlaisApplicationCrd) = ObjectMeta().apply {
    name = source.metadata.name
    namespace = source.metadata.namespace

    labels = source.metadata.labels.toMutableMap()
        .plus("app" to source.metadata.name)
        .plus(MANAGED_BY_FLAISERATOR_LABEL)
    annotations = mutableMapOf(
        DEPLOYMENT_CORRELATION_ID_ANNOTATION to source.metadata.annotations[DEPLOYMENT_CORRELATION_ID_ANNOTATION]
    )
    ownerReferences = listOf(createOwnerReference(source))
}

fun createOwnerReference(source: FlaisApplicationCrd) = OwnerReference().apply {
    apiVersion = source.apiVersion
    kind = source.kind
    name = source.metadata.name
    uid = source.metadata.uid
    controller = true
}