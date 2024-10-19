package no.fintlabs.operator

import io.javaoperatorsdk.operator.api.reconciler.Reconciler
import org.koin.core.qualifier.named
import org.koin.dsl.module

fun applicationReconcilerModule() = module {
    single<Reconciler<*>>(named("flais-application-reconciler")) { FlaisApplicationReconciler() }
    single { DeploymentDR() }
    single { ServiceDR() }
    single { PodMetricsDR() }
    single { OnePasswordDR() }
    single { IngressDR() }
    single { PostgresUserDR() }
    single { KafkaDR() }
    single { CreatePodMetricsCondition() }
    single { CreateOnePasswordCondition() }
    single { CreateIngressCondition() }
    single { CreatePostgresUserCondition() }
    single { CreateKafkaCondition() }
}