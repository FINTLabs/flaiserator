package no.fintlabs.operator.deployment;

import io.fabric8.kubernetes.api.model.EnvFromSource;
import io.fabric8.kubernetes.api.model.EnvFromSourceBuilder;
import io.fabric8.kubernetes.api.model.EnvVar;
import io.fabric8.kubernetes.api.model.EnvVarBuilder;
import no.fintlabs.FlaisKubernetesDependentResource;
import no.fintlabs.HasSecret;
import no.fintlabs.operator.FlaisApplicationCrd;
import no.fintlabs.operator.FlaisApplicationSpec;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public class EnvFromFactory {
    private final List<FlaisKubernetesDependentResource<?, FlaisApplicationCrd, FlaisApplicationSpec>> dependentResourcesWithSecret;

    public EnvFromFactory(List<FlaisKubernetesDependentResource<?, FlaisApplicationCrd, FlaisApplicationSpec>> dependentResourcesWithSecret) {
        this.dependentResourcesWithSecret = dependentResourcesWithSecret;
    }

    public List<EnvFromSource> getEnvFrom(FlaisApplicationCrd primary) {
        Set<EnvFromSource> envFrom = new HashSet<>(primary.getSpec().getEnvFrom());
        dependentResourcesWithSecret
                .stream()
                .filter(HasSecret::hasSecret)
                .filter(dr -> dr.shouldBeIncluded(primary))
                .forEach(withSecret -> {

                    envFrom.add(new EnvFromSourceBuilder().withNewSecretRef()
                            .withName(withSecret.getSecretName(primary))
                            .endSecretRef()
                            .build());
                });

        return envFrom.stream().toList();
    }

    public List<EnvVar> envs(FlaisApplicationCrd primary) {
        Set<EnvVar> envVars = new HashSet<>(primary.getSpec().getEnv());
        if (Objects.nonNull(primary.getSpec().getUrl().getBasePath())) {
            envVars.add(
                    new EnvVarBuilder()
                            .withName("spring.webflux.base-path")
                            .withValue(primary.getSpec().getUrl().getBasePath())
                            .build()
            );
        }
        return envVars.stream().toList();
    }
}
