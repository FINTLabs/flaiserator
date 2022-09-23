package no.fintlabs.operator;

import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.javaoperatorsdk.operator.api.reconciler.*;
import io.javaoperatorsdk.operator.api.reconciler.dependent.Dependent;
import lombok.extern.slf4j.Slf4j;
import no.fintlabs.operator.crd.FlaisApplicationCrd;
import no.fintlabs.operator.crd.FlaisApplicationStatus;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@ControllerConfiguration(
//        dependents = {
//                @Dependent(type = DeploymentDependentResource.class, reconcilePrecondition = CrdCondition.class),
//                @Dependent(type = ServiceDependentResource.class, reconcilePrecondition = CrdCondition.class)
//        },
//        generationAwareEventProcessing = false
        //genericFilter = VoidGenericFilter.class
)
public class FlaisApplicationReconiler implements Reconciler<FlaisApplicationCrd>, ErrorStatusHandler<FlaisApplicationCrd> {

    @Override
    public UpdateControl<FlaisApplicationCrd> reconcile(FlaisApplicationCrd resource, Context<FlaisApplicationCrd> context) {

        if (resource.getStatus() == null) resource.setStatus(new FlaisApplicationStatus());
        resource.getStatus().getDeployedResources().clear();
        resource.getStatus().setErrorMessage(null);

        context.getSecondaryResource(Deployment.class).ifPresent(deployment -> {
            log.info("Deployment {} is present", deployment.getMetadata().getName());
            resource.getStatus().getDeployedResources().put(Deployment.class.getSimpleName(), deployment.getMetadata().getNamespace() + "/" + deployment.getMetadata().getName());
        });

        context.getSecondaryResource(Service.class).ifPresent(service -> {
            log.info("Service {} is present", service.getMetadata().getName());
            resource.getStatus().getDeployedResources().put(Service.class.getSimpleName(), service.getMetadata().getNamespace() + "/" + service.getMetadata().getName());
        });

        return UpdateControl.patchStatus(resource);
    }

    @Override
    public ErrorStatusUpdateControl<FlaisApplicationCrd> updateErrorStatus(FlaisApplicationCrd resource, Context<FlaisApplicationCrd> context, Exception e) {
        FlaisApplicationStatus flaisApplicationStatus = new FlaisApplicationStatus();
        flaisApplicationStatus.setErrorMessage(e.getCause().getMessage());
        resource.setStatus(flaisApplicationStatus);
        return ErrorStatusUpdateControl.updateStatus(resource);
    }
}
