package no.fintlabs.application

import com.onepassword.v1.OnePasswordItem
import io.javaoperatorsdk.operator.api.reconciler.Reconciler
import no.fintlabs.application.api.v1alpha1.FlaisApplication
import no.fintlabs.common.KafkaDR
import no.fintlabs.common.OnePasswordDR
import no.fintlabs.v1alpha1.KafkaUserAndAcl
import org.koin.core.qualifier.named
import org.koin.dsl.module

fun applicationReconcilerModule() = module {
  single<Reconciler<*>>(named("flais-application-reconciler")) { ApplicationReconciler() }
  single { DeploymentDR() }
  single { ServiceDR() }
  single { PodMetricsDR() }
  single { OnePasswordDR<FlaisApplication>() }
  single { IngressDR() }
  single { PostgresUserDR() }
  single { KafkaDR<FlaisApplication>() }
}
