package no.fintlabs.application;

import io.fabric8.kubernetes.api.model.PodSpec;
import io.fabric8.kubernetes.api.model.PodSpecBuilder;
import io.fabric8.kubernetes.api.model.PodTemplateSpec;
import io.fabric8.kubernetes.api.model.PodTemplateSpecBuilder;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.api.model.apps.DeploymentBuilder;
import io.fabric8.kubernetes.api.model.apps.DeploymentSpec;
import io.fabric8.kubernetes.api.model.apps.DeploymentSpecBuilder;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.javaoperatorsdk.operator.api.reconciler.Context;
import io.javaoperatorsdk.operator.processing.dependent.kubernetes.KubernetesDependent;
import lombok.extern.slf4j.Slf4j;
import no.fintlabs.FlaisKubernetesDependentResource;
import no.fintlabs.LabelFactory;
import no.fintlabs.MetadataFactory;
import no.fintlabs.application.crd.FlaisApplicationCrd;
import no.fintlabs.application.crd.FlaisApplicationSpec;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@KubernetesDependent(labelSelector = "app.kubernetes.io/managed-by=flaiserator")
public class DeploymentDependentResource
        extends FlaisKubernetesDependentResource<Deployment, FlaisApplicationCrd, FlaisApplicationSpec> {

    private final MetadataFactory metadataFactory;

    public DeploymentDependentResource(FlaisApplicationWorkflow workflow, KubernetesClient kubernetesClient, MetadataFactory metadataFactory) {

        super(Deployment.class, workflow, kubernetesClient);

        this.metadataFactory = metadataFactory;
    }

    @Override
    protected Deployment desired(FlaisApplicationCrd resource, Context<FlaisApplicationCrd> context) {

        PodSpec podSpec = new PodSpecBuilder()
                .withRestartPolicy(resource.getSpec().getRestartPolicy())
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
                .build();

        PodTemplateSpec podTemplateSpec = new PodTemplateSpecBuilder()
                .withMetadata(metadataFactory.createObjectMetadataWithPrometheus(resource))
                .withSpec(podSpec)
                .build();

        DeploymentSpec deploymentSpec = new DeploymentSpecBuilder()
                .withReplicas(resource.getSpec().getReplicas())
                .withStrategy(resource.getSpec().getStrategy())
                .withNewSelector()
                .withMatchLabels(LabelFactory.createMatchLabels(resource))
                .endSelector()
                .withTemplate(podTemplateSpec)
                .build();

        log.info("Creating desired deployment...");
        Deployment deployment = new DeploymentBuilder()
                .withMetadata(metadataFactory.createObjectMetadata(resource))
                .withSpec(deploymentSpec)
                .build();

        deployment.getSpec().getTemplate().getSpec().getContainers().forEach(container -> container.getEnv().addAll(PropertyFactory.getStandardSpringBootProperties(resource)));
        return deployment;
    }
}
