package no.fintlabs.common.api.v1alpha1

import io.fabric8.kubernetes.api.model.Namespaced
import io.fabric8.kubernetes.client.CustomResource

abstract class FlaisResource<T> : CustomResource<T, FlaisResourceStatus>(), Namespaced
