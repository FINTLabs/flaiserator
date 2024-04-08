package no.fintlabs.operator.application.onepassword;

import io.javaoperatorsdk.operator.api.reconciler.Context;
import io.javaoperatorsdk.operator.processing.dependent.workflow.Condition;
import no.fintlabs.operator.application.FlaisApplicationCrd;

import java.util.Objects;

public class OnePasswordCondition implements Condition<OnePasswordCrd, FlaisApplicationCrd> {
    @Override
    public boolean isMet(FlaisApplicationCrd primary, OnePasswordCrd secondary, Context<FlaisApplicationCrd> context) {
        return Objects.nonNull(primary.getSpec().getOnePassword().getItemPath());
    }
}
