package no.fintlabs.operator.application.api

import io.fabric8.kubernetes.api.model.LocalObjectReference

class ImagePullSecret(name: String, val managed: Boolean = true) : LocalObjectReference(name)