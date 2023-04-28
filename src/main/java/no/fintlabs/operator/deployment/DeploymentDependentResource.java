package no.fintlabs.operator.deployment;

import io.fabric8.kubernetes.api.model.*;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.api.model.apps.DeploymentBuilder;
import io.fabric8.kubernetes.api.model.apps.DeploymentSpec;
import io.fabric8.kubernetes.api.model.apps.DeploymentSpecBuilder;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.javaoperatorsdk.operator.api.reconciler.Context;
import io.javaoperatorsdk.operator.processing.dependent.kubernetes.KubernetesDependent;
import io.javaoperatorsdk.operator.processing.dependent.kubernetes.KubernetesDependentResourceConfig;
import lombok.extern.slf4j.Slf4j;
import no.fintlabs.FlaisKubernetesDependentResource;
import no.fintlabs.operator.*;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@KubernetesDependent
public class DeploymentDependentResource
        extends FlaisKubernetesDependentResource<Deployment, FlaisApplicationCrd, FlaisApplicationSpec> {

    private final MetadataFactory metadataFactory;
    private final EnvFromFactory envFromFactory;

    public DeploymentDependentResource(FlaisApplicationWorkflow workflow,
                                       KubernetesClient kubernetesClient,
                                       MetadataFactory metadataFactory,
                                       List<FlaisKubernetesDependentResource<?, FlaisApplicationCrd, FlaisApplicationSpec>> dependentResourcesWithSecret) {

        super(Deployment.class, workflow, kubernetesClient);

        this.metadataFactory = metadataFactory;
        this.envFromFactory = new EnvFromFactory(dependentResourcesWithSecret);

        configureWith(
                new KubernetesDependentResourceConfig<Deployment>()
                        .setLabelSelector("app.kubernetes.io/managed-by=flaiserator")
        );
    }

    @Override
    protected Deployment desired(FlaisApplicationCrd resource, Context<FlaisApplicationCrd> context) {

        PodSpec podSpec = new PodSpecBuilder()
                .withVolumes(envFromFactory.volumes(resource))
                .withRestartPolicy(resource.getSpec().getRestartPolicy())
                .addNewContainer()
                .withVolumeMounts(envFromFactory.volumeMounts(resource))
                .withName(resource.getMetadata().getName())
                .withImage(resource.getSpec().getImage())
                .withImagePullPolicy(resource.getSpec().getImagePullPolicy())
                .withResources(resource.getSpec().getResources())
                .addNewPort()
                .withContainerPort(resource.getSpec().getPort())
                .endPort()
                .withEnv(envFromFactory.envs(resource))
                .withEnvFrom(envFromFactory.getEnvFrom(resource))
                .endContainer()
                .build();

        PodTemplateSpec podTemplateSpec = new PodTemplateSpecBuilder()
                .withMetadata(metadataFactory.metadataWithPrometheus(resource))
                .withSpec(podSpec)
                .build();

        DeploymentSpec deploymentSpec = new DeploymentSpecBuilder()
                .withReplicas(resource.getSpec().getReplicas())
                .withStrategy(resource.getSpec().getStrategy())
                .withNewSelector()
                .withMatchLabels(LabelFactory.recommendedLabels(resource))
                .endSelector()
                .withTemplate(podTemplateSpec)
                .build();

        log.info("Creating desired deployment...");
        Deployment deployment = new DeploymentBuilder()
                .withMetadata(metadataFactory.metadata(resource))
                .withSpec(deploymentSpec)
                .build();

        deployment.getSpec().getTemplate().getSpec().getContainers().forEach(container -> container.getEnv().addAll(PropertyFactory.standardProperties(resource)));

        return deployment;
    }
}
