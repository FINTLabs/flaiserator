package no.fintlabs.common

import io.javaoperatorsdk.operator.api.config.informer.Informer
import io.javaoperatorsdk.operator.api.reconciler.ControllerConfiguration
import no.fintlabs.operator.workflow.Dependent
import no.fintlabs.operator.workflow.Workflow

@ControllerConfiguration(
    fieldManager = "testreconciler",
    informer =
        Informer(
            onAddFilter = FlaisResourceReconciliationAddFilter::class,
            onUpdateFilter = FlaisResourceReconciliationUpdateFilter::class,
        ),
)
@Workflow(
    dependents =
        [
            Dependent(TestConfigDR::class),
            Dependent(KafkaDR::class),
            Dependent(PostgresUserDR::class),
            Dependent(OnePasswordDR::class),
        ]
)
class TestReconciler : FlaisResourceReconciler<FlaisTestResource>()
