package no.fintlabs.operator.application.ingress;

import io.fabric8.kubernetes.api.model.Namespaced;
import io.fabric8.kubernetes.client.CustomResource;
import io.fabric8.kubernetes.model.annotation.Group;
import io.fabric8.kubernetes.model.annotation.Kind;
import io.fabric8.kubernetes.model.annotation.Version;

@Group("traefik.containo.us")
@Version("v1alpha1")
@Kind("IngressRoute")
public class IngressRouteCrd extends CustomResource<IngressRouteSpec, Void> implements Namespaced {

    @Override
    protected IngressRouteSpec initSpec() {
        return new IngressRouteSpec();
    }
}
