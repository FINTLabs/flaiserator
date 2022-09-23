package no.fintlabs.operator;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.javaoperatorsdk.operator.api.reconciler.Context;
import io.javaoperatorsdk.operator.processing.dependent.workflow.Condition;
import no.fintlabs.operator.crd.FlaisApplicationCrd;

public class CrdCondition implements Condition<HasMetadata, FlaisApplicationCrd> {

    @Override
    public boolean isMet(FlaisApplicationCrd primary, HasMetadata secondary, Context<FlaisApplicationCrd> context) {
        return false;
    }
}
