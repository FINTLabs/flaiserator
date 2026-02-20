package no.fintlabs.application

import io.javaoperatorsdk.operator.api.config.informer.Informer
import io.javaoperatorsdk.operator.api.reconciler.ControllerConfiguration
import io.javaoperatorsdk.operator.processing.retry.GradualRetry
import no.fintlabs.application.api.v1alpha1.FlaisApplication
import no.fintlabs.common.FlaisResourceReconciler
import no.fintlabs.common.FlaisResourceReconciliationFilter
import no.fintlabs.common.KafkaDR
import no.fintlabs.common.OnePasswordDR
import no.fintlabs.common.PostgresUserDR
import no.fintlabs.operator.workflow.Dependent
import no.fintlabs.operator.workflow.Workflow
import org.koin.core.component.KoinComponent

@GradualRetry(maxAttempts = 3)
@ControllerConfiguration(
    informer =
        Informer(
            genericFilter = FlaisResourceReconciliationFilter::class,
        )
)
@Workflow(
    [
        Dependent(DeploymentDR::class),
        Dependent(ServiceDR::class),
        Dependent(PodMetricsDR::class),
        Dependent(IngressDR::class),
        Dependent(PostgresUserDR::class),
        Dependent(OnePasswordDR::class),
        Dependent(KafkaDR::class),
    ]
)
class ApplicationReconciler : FlaisResourceReconciler<FlaisApplication>(), KoinComponent
