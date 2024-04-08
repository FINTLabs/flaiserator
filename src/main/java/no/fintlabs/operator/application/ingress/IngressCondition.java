package no.fintlabs.operator.application.ingress;

import io.javaoperatorsdk.operator.api.reconciler.Context;
import io.javaoperatorsdk.operator.processing.dependent.workflow.Condition;
import no.fintlabs.operator.application.FlaisApplicationCrd;

import java.util.Objects;

public class IngressCondition implements Condition<IngressRouteCrd, FlaisApplicationCrd> {
    @Override
    public boolean isMet(FlaisApplicationCrd primary, IngressRouteCrd secondary, Context<FlaisApplicationCrd> context) {
        return primary.getSpec().getIngress().isEnabled()
                && Objects.nonNull(primary.getSpec().getUrl().getHostname())
                && Objects.nonNull(primary.getSpec().getUrl().getBasePath());
    }
}
