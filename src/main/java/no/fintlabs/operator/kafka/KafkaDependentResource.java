package no.fintlabs.operator.kafka;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.javaoperatorsdk.operator.api.reconciler.Context;
import io.javaoperatorsdk.operator.processing.dependent.DesiredEqualsMatcher;
import io.javaoperatorsdk.operator.processing.dependent.Matcher;
import io.javaoperatorsdk.operator.processing.dependent.kubernetes.GenericKubernetesResourceMatcher;
import io.javaoperatorsdk.operator.processing.dependent.kubernetes.KubernetesDependentResourceConfig;
import lombok.extern.slf4j.Slf4j;
import no.fintlabs.FlaisKubernetesDependentResource;
import no.fintlabs.operator.FlaisApplicationCrd;
import no.fintlabs.operator.FlaisApplicationSpec;
import no.fintlabs.operator.FlaisApplicationWorkflow;
import no.fintlabs.operator.LabelFactory;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class KafkaDependentResource
        extends FlaisKubernetesDependentResource<KafkaUserAndAclCrd, FlaisApplicationCrd, FlaisApplicationSpec> {
    public KafkaDependentResource(FlaisApplicationWorkflow workflow, KubernetesClient kubernetesClient) {
        super(KafkaUserAndAclCrd.class, workflow, new KafkaCondition(), kubernetesClient);
        configureWith(
                new KubernetesDependentResourceConfig<KafkaUserAndAclCrd>()
                        .setLabelSelector("app.kubernetes.io/managed-by=flaiserator")
        );

    }


    @Override
    protected KafkaUserAndAclCrd desired(FlaisApplicationCrd primary, Context<FlaisApplicationCrd> context) {

        KafkaUserAndAclCrd kafkaUserAndAclCrd = new KafkaUserAndAclCrd();
        kafkaUserAndAclCrd.getMetadata().setLabels(LabelFactory.recommendedLabels(primary));
        kafkaUserAndAclCrd.getMetadata().setName(primary.getMetadata().getName());
        kafkaUserAndAclCrd.getMetadata().setNamespace(primary.getMetadata().getNamespace());
        kafkaUserAndAclCrd.getSpec().setAcls(primary.getSpec().getKafka().getAcls());

        return kafkaUserAndAclCrd;
    }

    @Override
    public Matcher.Result<KafkaUserAndAclCrd> match(KafkaUserAndAclCrd actualResource, FlaisApplicationCrd primary, Context<FlaisApplicationCrd> context) {
        return GenericKubernetesResourceMatcher.match(this, actualResource, primary, context, false, true);
    }

    @Override
    public boolean hasSecret() {
        return true;
    }

    @Override
    public String getSecretName(HasMetadata primary) {
        return primary.getMetadata().getName() + "-kafka";
    }

    @Override
    public boolean shouldBeIncluded(FlaisApplicationCrd primary) {
        return primary.getSpec().getKafka().isEnabled();
    }
}
