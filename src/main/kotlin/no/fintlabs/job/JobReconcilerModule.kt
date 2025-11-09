package no.fintlabs.job

import io.javaoperatorsdk.operator.api.reconciler.Reconciler
import no.fintlabs.common.KafkaDR
import no.fintlabs.common.OnePasswordDR
import no.fintlabs.common.PostgresUserDR
import no.fintlabs.job.api.v1alpha1.FlaisJob
import org.koin.core.qualifier.named
import org.koin.dsl.module

fun jobReconcilerModule() = module {
  single<Reconciler<*>>(named("flais-job-reconciler")) { JobReconciler() }
  single { CronJobDR() }
  single { JobDR() }
  single { OnePasswordDR<FlaisJob>() }
  single { PostgresUserDR<FlaisJob>() }
  single { KafkaDR<FlaisJob>() }
}
