package no.fintlabs.operator.ingress;

import lombok.Data;

@Data
public class Ingress {
    private boolean enabled;
    private String basePath;
}
