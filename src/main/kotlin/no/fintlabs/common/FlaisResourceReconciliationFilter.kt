package no.fintlabs.common

import io.javaoperatorsdk.operator.processing.event.source.filter.OnAddFilter
import io.javaoperatorsdk.operator.processing.event.source.filter.OnUpdateFilter
import no.fintlabs.common.FlaisResourceReconciliationFilter.shouldReconcile
import no.fintlabs.common.api.v1alpha1.FlaisResource
import no.fintlabs.common.api.v1alpha1.FlaisResourceState
import no.fintlabs.common.api.v1alpha1.resourceHash
import no.fintlabs.common.utils.correlationIdAnnotation

class FlaisResourceReconciliationAddFilter : OnAddFilter<FlaisResource<*>> {
  override fun accept(resource: FlaisResource<*>): Boolean = shouldReconcile(resource)
}

class FlaisResourceReconciliationUpdateFilter : OnUpdateFilter<FlaisResource<*>> {
  override fun accept(newResource: FlaisResource<*>, oldResource: FlaisResource<*>): Boolean =
      shouldReconcile(newResource)

  override fun and(
      onUpdateFilter: OnUpdateFilter<FlaisResource<*>>
  ): OnUpdateFilter<FlaisResource<*>> {
    return super.or(onUpdateFilter)
  }
}

object FlaisResourceReconciliationFilter {
  fun shouldReconcile(resource: FlaisResource<*>): Boolean =
      resource.status == null ||
          resource.status.state == FlaisResourceState.PENDING ||
          resource.status.correlationId != resource.metadata.correlationIdAnnotation ||
          resource.status.observedGeneration != resource.metadata.generation ||
          (resource.status.synchronizationHash != null &&
              resource.status.synchronizationHash != resource.resourceHash())
}
