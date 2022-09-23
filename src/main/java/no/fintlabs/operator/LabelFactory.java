package no.fintlabs.operator;

import no.fintlabs.operator.crd.FlaisApplicationCrd;

import java.util.HashMap;
import java.util.Map;

public class LabelFactory {

    public static Map<String, String> updateRecommendedLabels(FlaisApplicationCrd resource) {
        Map<String, String> recommendedLabels = resource.getMetadata().getLabels();
        recommendedLabels.put("app.kubernetes.io/managed-by", "flaiserator");

        return recommendedLabels;
    }

    public static Map<String, String> createMatchLabels(FlaisApplicationCrd resource) {
        Map<String, String> matchLabels = new HashMap<>();

        matchLabels.put("app.kubernetes.io/name", resource.getMetadata().getLabels().get("app.kubernetes.io/name"));
        matchLabels.put("app.kubernetes.io/instance", resource.getMetadata().getLabels().get("app.kubernetes.io/instance"));

        return matchLabels;
    }

}
