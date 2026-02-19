package no.fintlabs.common

import io.javaoperatorsdk.operator.processing.event.source.filter.GenericFilter
import no.fintlabs.common.api.v1alpha1.FlaisResource
import no.fintlabs.common.api.v1alpha1.FlaisResourceState

class FlaisResourceReconciliationFilter : GenericFilter<FlaisResource<*>> {
  override fun accept(resource: FlaisResource<*>): Boolean {
    return resource.status == null
            || resource.status.state == FlaisResourceState.PENDING
            || resource.status.correlationId != resource.metadata.correlationIdAnnotation
            || resource.status.observedGeneration != resource.metadata.generation;
  }
}