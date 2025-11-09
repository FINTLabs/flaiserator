package no.fintlabs.common

import io.javaoperatorsdk.operator.api.reconciler.ControllerConfiguration
import no.fintlabs.operator.workflow.Dependent
import no.fintlabs.operator.workflow.Workflow

@ControllerConfiguration
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
