package no.fintlabs.application.crd;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import io.fabric8.kubernetes.api.model.EnvFromSource;
import io.fabric8.kubernetes.api.model.EnvVar;
import io.fabric8.kubernetes.api.model.ResourceRequirements;
import io.fabric8.kubernetes.api.model.apps.DeploymentStrategy;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import no.fintlabs.FlaisSpec;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class FlaisApplicationSpec implements FlaisSpec {

    @JsonPropertyDescription("If present this application is consider to be a county application.")
    private String orgId;

    @JsonPropertyDescription("")
    private List<EnvVar> env = new ArrayList<>();

    @JsonPropertyDescription("Same as envFrom in a Deployment.apps")
    private List<EnvFromSource> envFrom = new ArrayList<>();


    @JsonPropertyDescription("Same as image in Deployment.apps")
    private String image;

    @JsonPropertyDescription("Same as replicas in Deployment.apps")
    private Integer replicas;

    @JsonPropertyDescription("")
    private ResourceRequirements resources;

    @JsonPropertyDescription("")
    //@Builder.Default
    private Integer port = 8080;

    @JsonPropertyDescription("")
    //@Builder.Default
    private String restartPolicy = "Always";

    @JsonPropertyDescription("")
    private DeploymentStrategy strategy;

    @JsonPropertyDescription("")
    //@Builder.Default
    private Prometheus prometheus = new Prometheus();

}
