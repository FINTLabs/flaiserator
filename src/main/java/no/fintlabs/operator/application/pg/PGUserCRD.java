package no.fintlabs.operator.application.pg;

import io.fabric8.kubernetes.api.model.Namespaced;
import io.fabric8.kubernetes.model.annotation.Group;
import io.fabric8.kubernetes.model.annotation.Kind;
import io.fabric8.kubernetes.model.annotation.Version;
import no.fintlabs.FlaisCrd;

@Group("fintlabs.no")
@Version("v1alpha1")
@Kind("PGUser")
public class PGUserCRD extends FlaisCrd<PGUserSpec> implements Namespaced {

    @Override
    protected PGUserSpec initSpec() {
        return new PGUserSpec();
    }
}

