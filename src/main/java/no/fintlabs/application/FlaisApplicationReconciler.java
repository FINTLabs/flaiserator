package no.fintlabs.application;

import io.javaoperatorsdk.operator.api.reconciler.ControllerConfiguration;
import io.javaoperatorsdk.operator.api.reconciler.dependent.Deleter;
import io.javaoperatorsdk.operator.api.reconciler.dependent.EventSourceProvider;
import lombok.extern.slf4j.Slf4j;
import no.fintlabs.FlaisReconiler;
import no.fintlabs.FlaisWorkflow;
import no.fintlabs.application.crd.FlaisApplicationCrd;
import no.fintlabs.application.crd.FlaisApplicationSpec;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@ControllerConfiguration(
        generationAwareEventProcessing = true
)
public class FlaisApplicationReconciler extends FlaisReconiler<FlaisApplicationCrd, FlaisApplicationSpec> {
    public FlaisApplicationReconciler(FlaisWorkflow<FlaisApplicationCrd, FlaisApplicationSpec> workflow, List<? extends EventSourceProvider<FlaisApplicationCrd>> eventSourceProviders, List<? extends Deleter<FlaisApplicationCrd>> deleters) {
        super(workflow, eventSourceProviders, deleters);
    }
}
