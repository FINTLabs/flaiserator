package no.fintlabs.application.crd;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import io.fabric8.kubernetes.api.model.EnvFromSource;
import io.fabric8.kubernetes.api.model.EnvVar;
import io.fabric8.kubernetes.api.model.ResourceRequirements;
import io.fabric8.kubernetes.api.model.apps.DeploymentStrategy;
import lombok.*;
import no.fintlabs.FlaisSpec;
import no.fintlabs.onepassword.OnePassword;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
//@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FlaisApplicationSpec implements FlaisSpec {

    @JsonPropertyDescription("If present this application is consider to be a county application.")
    private String orgId;

    private List<EnvVar> env = new ArrayList<>();

    private List<EnvFromSource> envFrom = new ArrayList<>();

    private String image;

    private Integer replicas;

    private ResourceRequirements resources;

    private Integer port = 8080;

    private String restartPolicy = "Always";

    private DeploymentStrategy strategy;

    private Prometheus prometheus = new Prometheus();

    private OnePassword onePassword = new OnePassword();

}
