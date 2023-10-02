package no.fintlabs.operator.fint.adapter;

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

import java.util.Collections;

@Slf4j
@Component
public class FintAdapterDependentResource
        extends FlaisKubernetesDependentResource<FintAdapterCrd, FlaisApplicationCrd, FlaisApplicationSpec> {
    public FintAdapterDependentResource(FlaisApplicationWorkflow workflow, KubernetesClient kubernetesClient) {
        super(FintAdapterCrd.class, workflow, new FintAdapterCondition(), kubernetesClient);

        configureWith(
                new KubernetesDependentResourceConfig<FintAdapterCrd>()
                        .setLabelSelector("app.kubernetes.io/managed-by=flaiserator")
        );
    }

    @Override
    protected FintAdapterCrd desired(FlaisApplicationCrd primary, Context<FlaisApplicationCrd> context) {

        FintAdapterCrd fintAdapterCrd = new FintAdapterCrd();
        fintAdapterCrd.getMetadata().setLabels(LabelFactory.recommendedLabels(primary));
        fintAdapterCrd.getMetadata().setName(primary.getMetadata().getName());
        fintAdapterCrd.getMetadata().setNamespace(primary.getMetadata().getNamespace());
        fintAdapterCrd.getSpec().setAdapter(Collections.singletonList(primary.getSpec().getFint().getAdapter()));

        return fintAdapterCrd;
    }


    @Override
    public Matcher.Result<FintAdapterCrd> match(FintAdapterCrd actualResource, FlaisApplicationCrd primary, Context<FlaisApplicationCrd> context) {
        DesiredEqualsMatcher<FintAdapterCrd, FlaisApplicationCrd> matcher = new DesiredEqualsMatcher<>(this);
        return matcher.match(actualResource, primary, context);
    }
}
