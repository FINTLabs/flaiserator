package no.fintlabs.operator

import io.javaoperatorsdk.operator.api.reconciler.Reconciler
import org.koin.dsl.module

fun applicationReconcilerModule() = module {
    single<Reconciler<*>> { no.fintlabs.operator.FlaisApplicationReconciler() }
}