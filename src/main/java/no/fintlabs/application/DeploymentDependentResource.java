package no.fintlabs.application;

import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.api.model.apps.DeploymentBuilder;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.javaoperatorsdk.operator.api.reconciler.Context;
import io.javaoperatorsdk.operator.processing.dependent.kubernetes.KubernetesDependent;
import lombok.extern.slf4j.Slf4j;
import no.fintlabs.FlaisKubernetesDependentResource;
import no.fintlabs.LabelFactory;
import no.fintlabs.application.crd.FlaisApplicationCrd;
import no.fintlabs.application.crd.FlaisApplicationSpec;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@KubernetesDependent(labelSelector = "app.kubernetes.io/managed-by=flaiserator")
public class DeploymentDependentResource
        extends FlaisKubernetesDependentResource<Deployment, FlaisApplicationCrd, FlaisApplicationSpec> {

    public DeploymentDependentResource(FlaisApplicationWorkflow workflow, KubernetesClient kubernetesClient) {

        super(Deployment.class, workflow, kubernetesClient);

    }


    @Override
    protected Deployment desired(FlaisApplicationCrd resource, Context<FlaisApplicationCrd> context) {

        log.info("Creating desired deployment...");

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

        deployment.getSpec().getTemplate().getSpec().getContainers().forEach(container -> container.getEnv().addAll(PropertyFactory.getStandardSpringBootProperties(resource)));

        return deployment;
    }
}
