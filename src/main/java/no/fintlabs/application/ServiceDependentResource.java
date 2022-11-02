package no.fintlabs.application;

import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.api.model.ServiceBuilder;
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
public class ServiceDependentResource
        extends FlaisKubernetesDependentResource<Service, FlaisApplicationCrd, FlaisApplicationSpec> {

    public ServiceDependentResource(FlaisApplicationWorkflow workflow, KubernetesClient kubernetesClient) {
        super(Service.class, workflow, kubernetesClient);
    }


    @Override
    protected Service desired(FlaisApplicationCrd resource, Context<FlaisApplicationCrd> context) {

        log.info("Creating desired service...");

        Service service = new ServiceBuilder()
                .withNewMetadata()
                .withName(resource.getMetadata().getName())
                .withNamespace(resource.getMetadata().getNamespace())
                .withLabels(LabelFactory.updateRecommendedLabels(resource))
                .endMetadata()
                .withNewSpec()
                .addNewPort()
                .withPort(resource.getSpec().getPort())
                .endPort()
                .withType("ClusterIP")
                .withSelector(LabelFactory.createMatchLabels(resource))
                .endSpec()
                .build();

        log.debug("Desired service:");
        log.debug(service.toString());

        return service;
    }
}
