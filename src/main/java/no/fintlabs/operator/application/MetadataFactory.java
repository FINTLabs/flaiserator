package no.fintlabs.operator.application;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.api.model.ObjectMetaBuilder;
import org.springframework.stereotype.Component;

@Component
public class MetadataFactory {

    public ObjectMeta metadata(HasMetadata resource) {
        return new ObjectMetaBuilder()
                .withName(resource.getMetadata().getName())
                .withNamespace(resource.getMetadata().getNamespace())
                .withLabels(LabelFactory.recommendedLabels(resource))
                .build();
    }

    public ObjectMeta metadataWithPrometheus(FlaisApplicationCrd crd) {
        ObjectMeta objectMetadata = metadata(crd);
        objectMetadata.getAnnotations().putAll(crd.getSpec().getPrometheus().getPrometheusAnnotations());

        return objectMetadata;
    }

}
