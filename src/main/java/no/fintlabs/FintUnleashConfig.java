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

    @Value("${fint.unleash.apiKey#{null}}")
    private String apiKey;

    @Bean
    public DefaultUnleash unleash() {
        var builder = UnleashConfig.builder()
                .appName(applicationName)
                .unleashAPI(unleashApi);
        if (apiKey != null) {
            builder.apiKey(apiKey);
        }
        var config = builder.build();

        return new DefaultUnleash(config);
    }
}