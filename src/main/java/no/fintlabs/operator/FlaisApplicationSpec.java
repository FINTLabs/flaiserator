package no.fintlabs.operator;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import io.fabric8.kubernetes.api.model.*;
import io.fabric8.kubernetes.api.model.apps.DeploymentStrategy;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import no.fintlabs.FlaisSpec;
import no.fintlabs.operator.kafka.Kafka;
import no.fintlabs.operator.onepassword.OnePassword;
import no.fintlabs.operator.pg.Database;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class FlaisApplicationSpec implements FlaisSpec {

    @JsonPropertyDescription("If present this application is consider to be a county application.")
    private String orgId;

    private List<EnvVar> env = new ArrayList<>();

    private List<EnvFromSource> envFrom = new ArrayList<>();

    private String image;
    private String imagePullPolicy = "IfNotPresent";

    private Integer replicas = 1;

    private ResourceRequirements resources = new ResourceRequirementsBuilder()
            .withRequests(new HashMap<>() {{
                put("cpu", new Quantity("250m"));
                put("memory", new Quantity("256"));
            }})
            .withLimits(new HashMap<>() {{
                put("cpu", new Quantity("500m"));
                put("memory", new Quantity("512Mi"));
            }})
            .build();

    private Integer port = 8080;

    private String restartPolicy = "Always";

    private DeploymentStrategy strategy;

    private Prometheus prometheus = new Prometheus();

    private OnePassword onePassword = new OnePassword();

    private Kafka kafka = new Kafka();

    private Database database = new Database();

}
