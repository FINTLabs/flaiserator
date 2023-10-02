package no.fintlabs.operator.fint.adapter;

import io.javaoperatorsdk.operator.api.reconciler.Context;
import io.javaoperatorsdk.operator.processing.dependent.workflow.Condition;
import no.fintlabs.operator.FlaisApplicationCrd;

import java.util.Objects;

public class FintAdapterCondition implements Condition<FintAdapterCrd, FlaisApplicationCrd> {
    @Override
    public boolean isMet(FlaisApplicationCrd primary, FintAdapterCrd secondary, Context<FlaisApplicationCrd> context) {
        return Objects.nonNull(primary.getSpec().getFint().getAdapter().getShortDescription())
                && Objects.nonNull(primary.getSpec().getFint().getAdapter().getComponents());
    }
}
