package no.fintlabs.common.utils

import io.fabric8.kubernetes.api.model.HasMetadata
import io.javaoperatorsdk.operator.api.reconciler.BaseControl
import io.javaoperatorsdk.operator.api.reconciler.Context
import kotlin.jvm.optionals.getOrNull

fun <T : BaseControl<T>> BaseControl<T>.rescheduleImmediate(): T = rescheduleAfter(0L)

inline fun <reified P : HasMetadata> Context<*>.getSecondaryResource(): P? =
    this.getSecondaryResource<P>(P::class.java).getOrNull()

inline fun <reified P : HasMetadata> Context<*>.getRequiredSecondaryResource(): P =
    getSecondaryResource<P>().let {
      if (it == null) error("Required resource is null")
      it
    }
