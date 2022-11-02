package no.fintlabs.application.crd;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import io.fabric8.kubernetes.api.model.EnvFromSource;
import io.fabric8.kubernetes.api.model.EnvVar;
import io.fabric8.kubernetes.api.model.ResourceRequirements;
import io.fabric8.kubernetes.api.model.apps.DeploymentStrategy;
import lombok.*;
import no.fintlabs.FlaisSpec;

import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FlaisApplicationSpec implements FlaisSpec {

    @JsonPropertyDescription("If present this application is consider to be a county application.")
    private String orgId;

    @JsonPropertyDescription("")
    private List<EnvVar> env;

    @JsonPropertyDescription("")
    private List<EnvFromSource> envFrom;


    @JsonPropertyDescription("")
    private String image;

    @JsonPropertyDescription("")
    private Integer replicas;

    @JsonPropertyDescription("")
    private ResourceRequirements resources;

    @JsonPropertyDescription("")
    @Builder.Default
    private Integer port = 8080;

    @JsonPropertyDescription("")
    @Builder.Default
    private String restartPolicy = "Always";

    @JsonPropertyDescription("")
    private DeploymentStrategy strategy;

    @JsonPropertyDescription("")
    private Prometheus prometheus;

}
