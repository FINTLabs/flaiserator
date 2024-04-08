package no.fintlabs.operator.application.onepassword;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.javaoperatorsdk.operator.api.reconciler.Context;
import io.javaoperatorsdk.operator.processing.dependent.kubernetes.KubernetesDependentResourceConfig;
import lombok.extern.slf4j.Slf4j;
import no.fintlabs.FlaisKubernetesDependentResource;
import no.fintlabs.operator.application.FlaisApplicationCrd;
import no.fintlabs.operator.application.FlaisApplicationSpec;
import no.fintlabs.operator.application.FlaisApplicationWorkflow;
import no.fintlabs.operator.application.LabelFactory;
import org.springframework.stereotype.Component;

import java.util.Objects;

@Slf4j
@Component
public class OnePasswordDependentResource
        extends FlaisKubernetesDependentResource<OnePasswordCrd, FlaisApplicationCrd, FlaisApplicationSpec> {
    public OnePasswordDependentResource(FlaisApplicationWorkflow workflow, KubernetesClient kubernetesClient) {
        super(OnePasswordCrd.class, workflow, new OnePasswordCondition(), kubernetesClient);

        configureWith(
                new KubernetesDependentResourceConfig<OnePasswordCrd>()
                        .setLabelSelector("app.kubernetes.io/managed-by=flaiserator")
        );

    }


    @Override
    protected OnePasswordCrd desired(FlaisApplicationCrd primary, Context<FlaisApplicationCrd> context) {

        OnePasswordCrd onePasswordCrd = new OnePasswordCrd();
        onePasswordCrd.getMetadata().setLabels(LabelFactory.recommendedLabels(primary));
        onePasswordCrd.getMetadata().setName(getSecretName(primary));
        onePasswordCrd.getMetadata().setNamespace(primary.getMetadata().getNamespace());
        onePasswordCrd.getSpec().setItemPath(primary.getSpec().getOnePassword().getItemPath());

        return onePasswordCrd;
    }

    @Override
    public boolean hasSecret() {
        return true;
    }

    @Override
    public String getSecretName(HasMetadata primary) {
        return primary.getMetadata().getName() + "-op";
    }

    @Override
    public boolean shouldBeIncluded(FlaisApplicationCrd primary) {
        return Objects.nonNull(primary.getSpec().getOnePassword().getItemPath());
    }
}
