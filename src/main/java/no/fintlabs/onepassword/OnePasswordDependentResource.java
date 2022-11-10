package no.fintlabs.onepassword;

import io.fabric8.kubernetes.client.KubernetesClient;
import io.javaoperatorsdk.operator.api.reconciler.Context;
import io.javaoperatorsdk.operator.processing.dependent.kubernetes.KubernetesDependent;
import no.fintlabs.FlaisKubernetesDependentResource;
import no.fintlabs.FlaisWorkflow;
import no.fintlabs.LabelFactory;
import no.fintlabs.application.FlaisApplicationWorkflow;
import no.fintlabs.application.crd.FlaisApplicationCrd;
import no.fintlabs.application.crd.FlaisApplicationSpec;
import org.springframework.stereotype.Component;

@Component
@KubernetesDependent(labelSelector = "app.kubernetes.io/managed-by=flaiserator")
public class OnePasswordDependentResource
        extends FlaisKubernetesDependentResource<OnePasswordCrd, FlaisApplicationCrd, FlaisApplicationSpec> {
    public OnePasswordDependentResource(FlaisApplicationWorkflow workflow, KubernetesClient kubernetesClient) {
        super(OnePasswordCrd.class, workflow, kubernetesClient);
    }


    @Override
    protected OnePasswordCrd desired(FlaisApplicationCrd primary, Context<FlaisApplicationCrd> context) {

        OnePasswordCrd onePasswordCrd = new OnePasswordCrd();
        onePasswordCrd.getMetadata().setLabels(LabelFactory.updateRecommendedLabels(primary));
        onePasswordCrd.getMetadata().setName(primary.getMetadata().getName());
        onePasswordCrd.getMetadata().setNamespace(primary.getMetadata().getNamespace());
        onePasswordCrd.getSpec().setItemPath(primary.getSpec().getOnePassword().getItemPath());

        return onePasswordCrd;
    }
}
