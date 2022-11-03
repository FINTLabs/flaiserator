package no.fintlabs.onepassword;

import io.fabric8.kubernetes.api.model.GenericKubernetesResource;
import io.fabric8.kubernetes.api.model.GenericKubernetesResourceList;
import io.fabric8.kubernetes.api.model.KubernetesResourceList;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.dsl.MixedOperation;
import io.fabric8.kubernetes.client.dsl.Resource;
import io.javaoperatorsdk.operator.api.reconciler.Context;
import no.fintlabs.FlaisKubernetesDependentResource;
import no.fintlabs.FlaisWorkflow;
import no.fintlabs.LabelFactory;
import no.fintlabs.application.crd.FlaisApplicationCrd;
import no.fintlabs.application.crd.FlaisApplicationSpec;

public class OnePasswordDependentResource extends FlaisKubernetesDependentResource<OnePasswordCrd, FlaisApplicationCrd, FlaisApplicationSpec> {
    public OnePasswordDependentResource(FlaisWorkflow<FlaisApplicationCrd, FlaisApplicationSpec> workflow, KubernetesClient kubernetesClient) {
        super(OnePasswordCrd.class, workflow, kubernetesClient);
    }


    @Override
    protected OnePasswordCrd desired(FlaisApplicationCrd primary, Context<FlaisApplicationCrd> context) {

//        primary.getSpec().getOnePassword().getItemPaths().forEach(itemPath -> {
//
//        });
        OnePasswordCrd onePasswordCrd = new OnePasswordCrd();
        onePasswordCrd.getMetadata().setLabels(LabelFactory.updateRecommendedLabels(primary));
        onePasswordCrd.getMetadata().setName(primary.getMetadata().getName());
        onePasswordCrd.getMetadata().setNamespace(primary.getMetadata().getNamespace());
        onePasswordCrd.getSpec().setItemPath(primary.getSpec().getOnePassword().getItemPath());

        return onePasswordCrd;
    }
}
