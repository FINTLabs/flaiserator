package no.fintlabs.operator.crd;

import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Slf4j
public class CrdValidator {

    public static List<String> MANDATORY_LABELS = Arrays.asList(
            "app.kubernetes.io/name",
            "app.kubernetes.io/instance",
            "app.kubernetes.io/version",
            "app.kubernetes.io/component",
            "app.kubernetes.io/part-of",
            "fintlabs.no/team");

    public static void validate(FlaisApplicationCrd crd) {
        log.info("Validating CRD");
        List<String> missingLabels = new ArrayList<>(MANDATORY_LABELS);
        missingLabels.removeAll(crd.getMetadata().getLabels().keySet());

        if (missingLabels.size() > 0 ) {
            throw new IllegalArgumentException("The following mandatory labels is missing: \n- " + String.join("\n- ", missingLabels));
        }
    }
}
