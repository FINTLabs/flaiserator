package no.fintlabs.operator.ingress;

import io.fabric8.kubernetes.client.KubernetesClient;
import io.getunleash.DefaultUnleash;
import io.javaoperatorsdk.operator.api.reconciler.Context;
import io.javaoperatorsdk.operator.processing.dependent.DesiredEqualsMatcher;
import io.javaoperatorsdk.operator.processing.dependent.Matcher;
import io.javaoperatorsdk.operator.processing.dependent.kubernetes.KubernetesDependentResourceConfig;
import lombok.extern.slf4j.Slf4j;
import no.fintlabs.FlaisKubernetesDependentResource;
import no.fintlabs.operator.FlaisApplicationCrd;
import no.fintlabs.operator.FlaisApplicationSpec;
import no.fintlabs.operator.FlaisApplicationWorkflow;
import no.fintlabs.operator.LabelFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Slf4j
@Component
public class IngressDependentResource
        extends FlaisKubernetesDependentResource<IngressRouteCrd, FlaisApplicationCrd, FlaisApplicationSpec> {

    private final DefaultUnleash unleashClient;
    public IngressDependentResource(FlaisApplicationWorkflow workflow, KubernetesClient kubernetesClient, DefaultUnleash unleashClient) {
        super(IngressRouteCrd.class, workflow, new IngressCondition(), kubernetesClient);
        this.unleashClient = unleashClient;
        configureWith(
                new KubernetesDependentResourceConfig<IngressRouteCrd>()
                        .setLabelSelector("app.kubernetes.io/managed-by=flaiserator")
        );

    }


    @Override
    protected IngressRouteCrd desired(FlaisApplicationCrd primary, Context<FlaisApplicationCrd> context) {

        IngressRouteCrd ingressRouteCrd = new IngressRouteCrd();
        ingressRouteCrd.getMetadata().setLabels(LabelFactory.recommendedLabels(primary));
        ingressRouteCrd.getMetadata().setName(primary.getMetadata().getName());
        ingressRouteCrd.getMetadata().setNamespace(primary.getMetadata().getNamespace());
        IngressRouteSpec.Route route = new IngressRouteSpec.Route();
        route.setKind("Rule");
        route.setMatch(String.format("Host(`%s`) && PathPrefix(`%s`)",
                primary.getSpec().getUrl().getHostname(), basePath(primary)));
        IngressRouteSpec.Service service = new IngressRouteSpec.Service();
        service.setPort(primary.getSpec().getPort());
        service.setName(primary.getMetadata().getName());
        route.getServices().add(service);
        ingressRouteCrd.getSpec().getRoutes().add(route);
        ingressRouteCrd.getSpec().getEntryPoints().add("web");

        return ingressRouteCrd;
    }

    private String basePath(FlaisApplicationCrd primary) {

        if (unleashClient.isEnabled("flais.operators.flaiserator.ingress-path-override", false)) {
            log.info("'ingress-path-override' feature is enabled");
            if (StringUtils.hasText(primary.getSpec().getIngress().getBasePath())) {
                return primary.getSpec().getIngress().getBasePath();
            }
        }

        return primary.getSpec().getUrl().getBasePath();
    }

    @Override
    public Matcher.Result<IngressRouteCrd> match(IngressRouteCrd actualResource, FlaisApplicationCrd primary, Context<FlaisApplicationCrd> context) {
        DesiredEqualsMatcher<IngressRouteCrd, FlaisApplicationCrd> matcher = new DesiredEqualsMatcher<>(this);

        return matcher.match(actualResource, primary, context);
        //return super.match(actualResource, primary, context);
    }

//    @Override
//    public boolean hasSecret() {
//        return false;
//    }

//    @Override
//    public String getSecretName(HasMetadata primary) {
//        return primary.getMetadata().getName() + "-kafka";
//    }

//    @Override
//    public boolean shouldBeIncluded(FlaisApplicationCrd primary) {
//        return primary.getSpec().getIngress().isEnabled();
//    }
}
