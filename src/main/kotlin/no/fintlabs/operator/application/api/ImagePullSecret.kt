package no.fintlabs.operator.application.api

import io.fabric8.kubernetes.api.model.LocalObjectReference

class ImagePullSecret : LocalObjectReference() {
    val managed: Boolean = true
}