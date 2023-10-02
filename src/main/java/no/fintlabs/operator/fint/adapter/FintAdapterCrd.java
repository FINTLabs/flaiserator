package no.fintlabs.operator.fint.adapter;

import io.fabric8.kubernetes.api.model.Namespaced;
import io.fabric8.kubernetes.model.annotation.Group;
import io.fabric8.kubernetes.model.annotation.Kind;
import io.fabric8.kubernetes.model.annotation.Version;
import no.fintlabs.FlaisCrd;

@Group("fintlabs.no")
@Version("v1alpha1")
@Kind("FintAdapter")
public class FintAdapterCrd extends FlaisCrd<FintAdapterSpec> implements Namespaced {
    @Override
    protected FintAdapterSpec initSpec() {
        return new FintAdapterSpec();
    }
}
