package no.fintlabs.operator.application

import io.javaoperatorsdk.operator.api.reconciler.Reconciler
import org.koin.dsl.module

fun applicationReconcilerModule() = module {
    single<Reconciler<*>> { FlaisApplicationReconciler() }
}