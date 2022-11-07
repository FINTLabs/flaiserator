package no.fintlabs.onepassword;

import io.fabric8.kubernetes.api.model.KubernetesResource;
import lombok.Data;

import java.io.Serializable;

@Data
public class OnePasswordSpec {
    private String itemPath;
}
