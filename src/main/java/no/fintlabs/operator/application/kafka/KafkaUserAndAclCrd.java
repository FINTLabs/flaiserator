package no.fintlabs.operator.application.kafka;

import io.fabric8.kubernetes.api.model.Namespaced;
import io.fabric8.kubernetes.model.annotation.Group;
import io.fabric8.kubernetes.model.annotation.Kind;
import io.fabric8.kubernetes.model.annotation.Version;
import no.fintlabs.FlaisCrd;

@Group("fintlabs.no")
@Version("v1alpha1")
@Kind("KafkaUserAndAcl")
public class KafkaUserAndAclCrd extends FlaisCrd<KafkaUserAndAclSpec> implements Namespaced {
    @Override
    protected KafkaUserAndAclSpec initSpec() {
        return new KafkaUserAndAclSpec();
    }
}

