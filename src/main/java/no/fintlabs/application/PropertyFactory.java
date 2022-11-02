package no.fintlabs.application;

import io.fabric8.kubernetes.api.model.EnvVar;
import io.fabric8.kubernetes.api.model.EnvVarBuilder;
import no.fintlabs.application.crd.FlaisApplicationCrd;

import java.util.ArrayList;
import java.util.List;

public class PropertyFactory {

    public static List<EnvVar> getStandardSpringBootProperties(FlaisApplicationCrd resource) {

        List<EnvVar> props = new ArrayList<>();

        int memory = Integer.parseInt(resource.getSpec().getResources().getLimits().get("memory").getAmount());
        props.add(new EnvVarBuilder().withName("TZ").withValue("Europe/Oslo").build());
        props.add(new EnvVarBuilder().withName("fint.org-id").withValue(resource.getSpec().getOrgId()).build());
        props.add(new EnvVarBuilder()
                .withName("JAVA_TOOL_OPTIONS").
                withValue("-XX:+ExitOnOutOfMemoryError -Xmx" + Double.valueOf(memory * 0.9).intValue() + "M").build()
        );


        return props;
    }
}
