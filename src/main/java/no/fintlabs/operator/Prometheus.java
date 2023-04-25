package no.fintlabs.operator;

import lombok.*;

import java.util.HashMap;
import java.util.Map;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Prometheus {

    private boolean enabled = true;

    @Builder.Default
    private String path = "/actuator/prometheus";

    @Builder.Default
    private String port = "8080";

    public  Map<String, String> getPrometheusAnnotations() {
        Map<String, String> annotations = new HashMap<>();

        if (enabled) {
            annotations.put("prometheus.io/scrape", String.valueOf(enabled));
            annotations.put("prometheus.io/port", port);
            annotations.put("prometheus.io/path", path);
        }
        return annotations;
    }
}
