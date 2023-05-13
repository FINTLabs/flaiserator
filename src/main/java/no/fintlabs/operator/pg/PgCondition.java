package no.fintlabs.operator.pg;

import io.javaoperatorsdk.operator.api.reconciler.Context;
import io.javaoperatorsdk.operator.processing.dependent.workflow.Condition;
import no.fintlabs.operator.FlaisApplicationCrd;
import org.springframework.util.StringUtils;

public class PgCondition implements Condition<PGUserCRD, FlaisApplicationCrd> {
    @Override
    public boolean isMet(FlaisApplicationCrd primary, PGUserCRD secondary, Context<FlaisApplicationCrd> context) {
        return StringUtils.hasText(primary.getSpec().getDatabase().getDatabase());
    }
}
