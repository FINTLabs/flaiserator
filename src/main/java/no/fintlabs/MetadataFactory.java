package no.fintlabs;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.api.model.ObjectMetaBuilder;
import no.fintlabs.application.crd.FlaisApplicationCrd;
import org.springframework.stereotype.Component;

@Component
public class MetadataFactory {

    public ObjectMeta createObjectMetadata(HasMetadata resource) {
        return new ObjectMetaBuilder()
                .withName(resource.getMetadata().getName())
                .withNamespace(resource.getMetadata().getNamespace())
                .withLabels(LabelFactory.updateRecommendedLabels(resource))
                .build();
    }

    public ObjectMeta createObjectMetadataWithPrometheus(FlaisApplicationCrd crd) {
        ObjectMeta objectMetadata = createObjectMetadata(crd);
        objectMetadata.getAnnotations().putAll(crd.getSpec().getPrometheus().getPrometheusAnnotations());

        return objectMetadata;
    }

}
