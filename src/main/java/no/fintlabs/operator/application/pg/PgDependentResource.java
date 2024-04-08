package no.fintlabs.operator.application.pg;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.javaoperatorsdk.operator.api.reconciler.Context;
import io.javaoperatorsdk.operator.processing.dependent.Matcher;
import io.javaoperatorsdk.operator.processing.dependent.kubernetes.GenericKubernetesResourceMatcher;
import io.javaoperatorsdk.operator.processing.dependent.kubernetes.KubernetesDependentResourceConfig;
import lombok.extern.slf4j.Slf4j;
import no.fintlabs.FlaisKubernetesDependentResource;
import no.fintlabs.operator.application.FlaisApplicationCrd;
import no.fintlabs.operator.application.FlaisApplicationSpec;
import no.fintlabs.operator.application.FlaisApplicationWorkflow;
import no.fintlabs.operator.application.LabelFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Slf4j
@Component
public class PgDependentResource
        extends FlaisKubernetesDependentResource<PGUserCRD, FlaisApplicationCrd, FlaisApplicationSpec> {
    public PgDependentResource(FlaisApplicationWorkflow workflow, KubernetesClient kubernetesClient) {
        super(PGUserCRD.class, workflow, new PgCondition(), kubernetesClient);
        configureWith(
                new KubernetesDependentResourceConfig<PGUserCRD>()
                        .setLabelSelector("app.kubernetes.io/managed-by=flaiserator")
        );

    }


    @Override
    protected PGUserCRD desired(FlaisApplicationCrd primary, Context<FlaisApplicationCrd> context) {

        PGUserCRD pgUserCRD = new PGUserCRD();
        pgUserCRD.getMetadata().setLabels(LabelFactory.recommendedLabels(primary));
        pgUserCRD.getMetadata().setName(getSecretName(primary));
        pgUserCRD.getMetadata().setNamespace(primary.getMetadata().getNamespace());
        pgUserCRD.getSpec().setDatabase(primary.getSpec().getDatabase().getDatabase());

        return pgUserCRD;
    }

    @Override
    public Matcher.Result<PGUserCRD> match(PGUserCRD actualResource, FlaisApplicationCrd primary, Context<FlaisApplicationCrd> context) {
        return GenericKubernetesResourceMatcher.match(this, actualResource, primary, context, false, true);
    }

    @Override
    public boolean hasSecret() {
        return true;
    }

    @Override
    public String getSecretName(HasMetadata primary) {
        return primary.getMetadata().getName() + "-db";
    }

    @Override
    public boolean shouldBeIncluded(FlaisApplicationCrd primary) {
        return StringUtils.hasText(primary.getSpec().getDatabase().getDatabase());
    }
}
