package no.fintlabs.application

import io.javaoperatorsdk.operator.api.reconciler.Reconciler
import no.fintlabs.application.api.v1alpha1.FlaisApplication
import no.fintlabs.common.KafkaDR
import no.fintlabs.common.OnePasswordDR
import no.fintlabs.common.PostgresUserDR
import no.fintlabs.common.pod.PodBuilder
import org.koin.core.qualifier.named
import org.koin.dsl.module

fun applicationReconcilerModule() = module {
  single<Reconciler<*>>(named("flais-application-reconciler")) { ApplicationReconciler() }
  single { DeploymentDR() }
  single { ServiceDR() }
  single { PodMetricsDR() }
  single { IngressDR() }
  single { OnePasswordDR<FlaisApplication>() }
  single { PostgresUserDR<FlaisApplication>() }
  single { KafkaDR<FlaisApplication>() }
}
