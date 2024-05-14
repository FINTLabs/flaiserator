package no.fintlabs.operator.application

import io.fabric8.kubernetes.api.model.ObjectMeta
import io.fabric8.kubernetes.api.model.OwnerReference
import no.fintlabs.operator.application.api.FlaisApplicationCrd

fun createObjectMeta(source: FlaisApplicationCrd) = ObjectMeta().apply {
    name = source.metadata.name
    namespace = source.metadata.namespace

    labels = source.metadata.labels.toMutableMap()
        .plus("app" to source.metadata.name)
        .plus("app.kubernetes.io/managed-by" to "flaiserator")
    annotations = mutableMapOf()
    ownerReferences = listOf(createOwnerReference(source))
}

fun createOwnerReference(source: FlaisApplicationCrd) = OwnerReference().apply {
    apiVersion = source.apiVersion
    kind = source.kind
    name = source.metadata.name
    uid = source.metadata.uid
    controller = true
}