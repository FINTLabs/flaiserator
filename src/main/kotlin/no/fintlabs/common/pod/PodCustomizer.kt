package no.fintlabs.common.pod

import io.javaoperatorsdk.operator.api.reconciler.Context
import no.fintlabs.common.api.v1alpha1.FlaisResource

interface PodCustomizer<T : FlaisResource<*>> {
  fun customizePod(primary: T, builderContext: PodBuilderContext, context: Context<T>)
}
