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

    @Value("${fint.unleash.api}")
    private String unleashApi;

    @Value("${fint.unleash.apiKey}")
    private String apiKey;

    @Bean
    public DefaultUnleash unleash() {
        UnleashConfig config = UnleashConfig.builder()
                .appName(applicationName)
                .unleashAPI(unleashApi)
                .apiKey(apiKey)
                .build();

        return new DefaultUnleash(config);
    }
}