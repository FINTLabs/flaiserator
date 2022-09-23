package no.fintlabs.operator;

import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.api.model.apps.DeploymentBuilder;
import io.javaoperatorsdk.operator.api.reconciler.Context;
import io.javaoperatorsdk.operator.processing.dependent.kubernetes.CRUDKubernetesDependentResource;
import io.javaoperatorsdk.operator.processing.dependent.kubernetes.KubernetesDependent;
import lombok.extern.slf4j.Slf4j;
import no.fintlabs.operator.crd.CrdValidator;
import no.fintlabs.operator.crd.FlaisApplicationCrd;

@Slf4j
@KubernetesDependent(labelSelector = "app.kubernetes.io/managed-by=flaiserator")
public class DeploymentDependentResource
        extends CRUDKubernetesDependentResource<Deployment, FlaisApplicationCrd> {

    public DeploymentDependentResource() {

        super(Deployment.class);

    }


    @Override
    protected Deployment desired(FlaisApplicationCrd resource, Context<FlaisApplicationCrd> context) {

        log.info("Creating desired deployment...");
        CrdValidator.validate(resource);

        Deployment deployment = new DeploymentBuilder()
                .withNewMetadata()
                .withName(resource.getMetadata().getName())
                .withNamespace(resource.getMetadata().getNamespace())
                .withLabels(LabelFactory.updateRecommendedLabels(resource))
                .endMetadata()
                .withNewSpec()
                .withReplicas(resource.getSpec().getReplicas())
                .withStrategy(resource.getSpec().getStrategy())
                .withNewSelector()
                .withMatchLabels(LabelFactory.createMatchLabels(resource))
                .endSelector()
                .withNewTemplate()
                .withNewMetadata()
                .withLabels(LabelFactory.updateRecommendedLabels(resource))
                .endMetadata()
                .withNewSpec()
                .withRestartPolicy(resource.getSpec().getRestartPolicy())
                .withContainers()
                .addNewContainer()
                .withName(resource.getMetadata().getName())
                .withImage(resource.getSpec().getImage())
                .withResources(resource.getSpec().getResources())
                .addNewPort()
                .withContainerPort(resource.getSpec().getPort())
                .endPort()
                .withEnv(resource.getSpec().getEnv())
                .withEnvFrom(resource.getSpec().getEnvFrom())
                .endContainer()
                .endSpec()
                .endTemplate()
                .endSpec()
                .build();

        deployment.getSpec().getTemplate().getSpec().getContainers().forEach(container -> {
            container.getEnv().addAll(PropertyFactory.getStandardSpringBootProperties(resource));
        });

        return deployment;
    }
}
