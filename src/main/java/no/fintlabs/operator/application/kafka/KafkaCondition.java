package no.fintlabs.operator.application.kafka;

import io.javaoperatorsdk.operator.api.reconciler.Context;
import io.javaoperatorsdk.operator.processing.dependent.workflow.Condition;
import no.fintlabs.operator.application.FlaisApplicationCrd;

public class KafkaCondition implements Condition<KafkaUserAndAclCrd, FlaisApplicationCrd> {
    @Override
    public boolean isMet(FlaisApplicationCrd primary, KafkaUserAndAclCrd secondary, Context<FlaisApplicationCrd> context) {
        return primary.getSpec().getKafka().isEnabled();
    }
}
