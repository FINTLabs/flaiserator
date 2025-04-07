package no.fintlabs.application

import io.fabric8.kubernetes.api.model.ObjectMeta
import io.fabric8.kubernetes.api.model.OwnerReference
import no.fintlabs.application.api.DEPLOYMENT_CORRELATION_ID_ANNOTATION
import no.fintlabs.application.api.v1alpha1.FlaisApplicationCrd
import no.fintlabs.application.api.MANAGED_BY_FLAISERATOR_LABEL
import org.slf4j.Logger
import org.slf4j.LoggerFactory

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

inline fun <reified T> T.getLogger(): Logger = LoggerFactory.getLogger(T::class.java)