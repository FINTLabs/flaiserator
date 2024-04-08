package no.fintlabs.operator.application.ingress;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class Ingress {
    private boolean enabled;
    private String basePath;
    private List<String> middlewares = new ArrayList<>();
}
