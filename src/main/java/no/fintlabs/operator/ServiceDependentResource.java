package no.fintlabs.operator;

import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.api.model.ServiceBuilder;
import io.javaoperatorsdk.operator.api.reconciler.Context;
import io.javaoperatorsdk.operator.processing.dependent.kubernetes.CRUDKubernetesDependentResource;
import io.javaoperatorsdk.operator.processing.dependent.kubernetes.KubernetesDependent;
import lombok.extern.slf4j.Slf4j;
import no.fintlabs.operator.crd.CrdValidator;
import no.fintlabs.operator.crd.FlaisApplicationCrd;

@Slf4j
@KubernetesDependent(labelSelector = "app.kubernetes.io/managed-by=flaiserator")
public class ServiceDependentResource
        extends CRUDKubernetesDependentResource<Service, FlaisApplicationCrd> {

    public ServiceDependentResource() {
        super(Service.class);
    }


    @Override
    protected Service desired(FlaisApplicationCrd resource, Context<FlaisApplicationCrd> context) {

        log.info("Creating desired service...");
        CrdValidator.validate(resource);

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

        return service;
    }
}
