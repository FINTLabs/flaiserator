package no.fintlabs.operator.deployment;

import io.fabric8.kubernetes.api.model.*;
import no.fintlabs.FlaisKubernetesDependentResource;
import no.fintlabs.HasSecret;
import no.fintlabs.operator.FlaisApplicationCrd;
import no.fintlabs.operator.FlaisApplicationSpec;

import java.util.*;

public class DeploymentFactory {
    private final List<FlaisKubernetesDependentResource<?, FlaisApplicationCrd, FlaisApplicationSpec>> dependentResourcesWithSecret;

    public DeploymentFactory(List<FlaisKubernetesDependentResource<?, FlaisApplicationCrd, FlaisApplicationSpec>> dependentResourcesWithSecret) {
        this.dependentResourcesWithSecret = dependentResourcesWithSecret;
    }

    public List<EnvFromSource> getEnvFrom(FlaisApplicationCrd primary) {
        Set<EnvFromSource> envFrom = new HashSet<>(primary.getSpec().getEnvFrom());
        dependentResourcesWithSecret
                .stream()
                .filter(HasSecret::hasSecret)
                .filter(dr -> dr.shouldBeIncluded(primary))
                .forEach(withSecret -> envFrom.add(new EnvFromSourceBuilder().withNewSecretRef()
                        .withName(withSecret.getSecretName(primary))
                        .endSecretRef()
                        .build()));

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

    public List<Volume> volumes(FlaisApplicationCrd primary) {
        List<Volume> volumes = new ArrayList<>();
        if (primary.getSpec().getKafka().isEnabled()) {
            volumes.add(new VolumeBuilder()
                    .withName("credentials")
                    .withSecret(new SecretVolumeSourceBuilder()
                            .withSecretName(primary.getMetadata().getName() + "-kafka-certificates")
                            .build())
                    .build());
        }
        return volumes;
    }
    public List<VolumeMount> volumeMounts(FlaisApplicationCrd primary) {
        List<VolumeMount> volumeMounts = new ArrayList<>();
        if (primary.getSpec().getKafka().isEnabled()) {
            volumeMounts.add(new VolumeMountBuilder()
                    .withName("credentials")
                    .withMountPath("/credentials")
                    .build());
        }

        return volumeMounts;
    }
}
