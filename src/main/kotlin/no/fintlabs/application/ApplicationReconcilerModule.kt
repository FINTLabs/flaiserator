package no.fintlabs.application

import io.javaoperatorsdk.operator.api.reconciler.Reconciler
import org.koin.core.qualifier.named
import org.koin.dsl.module

fun applicationReconcilerModule() = module {
    single<Reconciler<*>>(named("flais-application-reconciler")) { ApplicationReconciler() }
    single { DeploymentDR() }
    single { ServiceDR() }
    single { PodMetricsDR() }
    single { OnePasswordDR() }
    single { IngressDR() }
    single { PostgresUserDR() }
    single { KafkaDR() }
}