package no.fintlabs;

import io.getunleash.DefaultUnleash;
import io.getunleash.util.UnleashConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class FintUnleashConfig {

    @Value("${spring.application.name}")
    private String applicationName;

    @Value("${fint.unleash.api:https://unleash-alpha.fintlabs.no/api/}")
    private String unleashApi;

    @Value("${fint.unleash.instance-id:flaiserator-operator}")
    private String instanceId;

    @Bean
    public DefaultUnleash unleash() {
        UnleashConfig config = UnleashConfig.builder()
                .appName(applicationName)
                .instanceId(instanceId)
                .unleashAPI(unleashApi)
                .build();

        return new DefaultUnleash(config);
    }
}