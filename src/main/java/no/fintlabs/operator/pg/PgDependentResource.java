package no.fintlabs.operator.pg;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.javaoperatorsdk.operator.api.reconciler.Context;
import io.javaoperatorsdk.operator.processing.dependent.DesiredEqualsMatcher;
import io.javaoperatorsdk.operator.processing.dependent.Matcher;
import io.javaoperatorsdk.operator.processing.dependent.kubernetes.KubernetesDependentResourceConfig;
import lombok.extern.slf4j.Slf4j;
import no.fintlabs.FlaisKubernetesDependentResource;
import no.fintlabs.operator.FlaisApplicationCrd;
import no.fintlabs.operator.FlaisApplicationSpec;
import no.fintlabs.operator.FlaisApplicationWorkflow;
import no.fintlabs.operator.LabelFactory;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class PgDependentResource
        extends FlaisKubernetesDependentResource<PGDatabaseAndUserCRD, FlaisApplicationCrd, FlaisApplicationSpec> {
    public PgDependentResource(FlaisApplicationWorkflow workflow, KubernetesClient kubernetesClient) {
        super(PGDatabaseAndUserCRD.class, workflow, new PgCondition(), kubernetesClient);
        configureWith(
                new KubernetesDependentResourceConfig<PGDatabaseAndUserCRD>()
                        .setLabelSelector("app.kubernetes.io/managed-by=flaiserator")
        );

    }


    @Override
    protected PGDatabaseAndUserCRD desired(FlaisApplicationCrd primary, Context<FlaisApplicationCrd> context) {

        PGDatabaseAndUserCRD pgDatabaseAndUserCRD = new PGDatabaseAndUserCRD();
        pgDatabaseAndUserCRD.getMetadata().setLabels(LabelFactory.recommendedLabels(primary));
        pgDatabaseAndUserCRD.getMetadata().setName(getSecretName(primary));
        pgDatabaseAndUserCRD.getMetadata().setNamespace(primary.getMetadata().getNamespace());

        return pgDatabaseAndUserCRD;
    }

    @Override
    public Matcher.Result<PGDatabaseAndUserCRD> match(PGDatabaseAndUserCRD actualResource, FlaisApplicationCrd primary, Context<FlaisApplicationCrd> context) {
        DesiredEqualsMatcher<PGDatabaseAndUserCRD, FlaisApplicationCrd> matcher = new DesiredEqualsMatcher<>(this);

        return matcher.match(actualResource, primary, context);
        //return super.match(actualResource, primary, context);
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
        return primary.getSpec().getDatabase().isEnabled();
    }
}
