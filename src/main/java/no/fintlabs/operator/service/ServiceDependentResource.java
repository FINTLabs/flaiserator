package no.fintlabs.operator.service;

import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.api.model.ServiceBuilder;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.javaoperatorsdk.operator.api.reconciler.Context;
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
public class ServiceDependentResource
        extends FlaisKubernetesDependentResource<Service, FlaisApplicationCrd, FlaisApplicationSpec> {

    public ServiceDependentResource(FlaisApplicationWorkflow workflow, KubernetesClient kubernetesClient) {
        super(Service.class, workflow, kubernetesClient);

        configureWith(
                new KubernetesDependentResourceConfig<Service>()
                        .setLabelSelector("app.kubernetes.io/managed-by=flaiserator")
        );
    }


    @Override
    protected Service desired(FlaisApplicationCrd resource, Context<FlaisApplicationCrd> context) {

        log.info("Creating desired service...");

        Service service = new ServiceBuilder()
                .withNewMetadata()
                .withName(resource.getMetadata().getName())
                .withNamespace(resource.getMetadata().getNamespace())
                .withLabels(LabelFactory.recommendedLabels(resource))
                .endMetadata()
                .withNewSpec()
                .addNewPort()
                .withPort(resource.getSpec().getPort())
                .endPort()
                .withType("ClusterIP")
                .withSelector(LabelFactory.recommendedLabels(resource))
                .endSpec()
                .build();

        log.debug("Desired service:");
        log.debug(service.toString());

        return service;
    }
}
