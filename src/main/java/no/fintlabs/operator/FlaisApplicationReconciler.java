package no.fintlabs.operator;

import io.javaoperatorsdk.operator.api.reconciler.ControllerConfiguration;
import io.javaoperatorsdk.operator.api.reconciler.dependent.Deleter;
import io.javaoperatorsdk.operator.api.reconciler.dependent.DependentResource;
import lombok.extern.slf4j.Slf4j;
import no.fintlabs.FlaisReconiler;
import no.fintlabs.FlaisWorkflow;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@ControllerConfiguration
public class FlaisApplicationReconciler extends FlaisReconiler<FlaisApplicationCrd, FlaisApplicationSpec> {
    public FlaisApplicationReconciler(FlaisWorkflow<FlaisApplicationCrd,
            FlaisApplicationSpec> workflow,
                                      List<? extends DependentResource<?,FlaisApplicationCrd>> eventSourceProviders,
                                      List<? extends Deleter<FlaisApplicationCrd>> deleters) {
        super(workflow, eventSourceProviders, deleters);
    }
}
