package no.fintlabs.operator.crd;

import io.javaoperatorsdk.operator.api.ObservedGenerationAware;
import lombok.*;

import java.util.HashMap;
import java.util.Map;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FlaisApplicationStatus implements ObservedGenerationAware {

    private String errorMessage;
    private Map<String, String> deployedResources = new HashMap<>();
    private Long observedGeneration;

    @Override
    public void setObservedGeneration(Long generation) {
        observedGeneration = generation;
    }

    @Override
    public Long getObservedGeneration() {
        return observedGeneration;
    }
}
