package no.fintlabs.operator.pg;

import io.javaoperatorsdk.operator.api.reconciler.Context;
import io.javaoperatorsdk.operator.processing.dependent.workflow.Condition;
import no.fintlabs.operator.FlaisApplicationCrd;

public class PgCondition implements Condition<PGDatabaseAndUserCRD, FlaisApplicationCrd> {
    @Override
    public boolean isMet(FlaisApplicationCrd primary, PGDatabaseAndUserCRD secondary, Context<FlaisApplicationCrd> context) {
        return primary.getSpec().getDatabase().isEnabled();
    }
}
