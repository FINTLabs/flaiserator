package no.fintlabs.operator.deployment;

import io.fabric8.kubernetes.api.model.EnvFromSource;
import io.fabric8.kubernetes.api.model.EnvFromSourceBuilder;
import no.fintlabs.FlaisKubernetesDependentResource;
import no.fintlabs.HasSecret;
import no.fintlabs.operator.FlaisApplicationCrd;
import no.fintlabs.operator.FlaisApplicationSpec;

import java.util.List;

public class EnvFromFactory {
    private final List<FlaisKubernetesDependentResource<?, FlaisApplicationCrd, FlaisApplicationSpec>> dependentResourcesWithSecret;

    public EnvFromFactory(List<FlaisKubernetesDependentResource<?, FlaisApplicationCrd, FlaisApplicationSpec>> dependentResourcesWithSecret) {
        this.dependentResourcesWithSecret = dependentResourcesWithSecret;
    }

    public List<EnvFromSource> getEnvFrom(FlaisApplicationCrd primary) {
        List<EnvFromSource> envFrom = primary.getSpec().getEnvFrom();

        dependentResourcesWithSecret
                .stream()
                .filter(HasSecret::hasSecret)
                .filter(dr -> dr.shouldBeIncluded(primary))
                .forEach(withSecret -> envFrom.add(new EnvFromSourceBuilder().withNewSecretRef()
                        .withName(withSecret.getSecretName(primary))
                        .endSecretRef()
                        .build()));

        return envFrom;
    }
}
