package no.fintlabs

import com.sksamuel.hoplite.ConfigLoaderBuilder
import com.sksamuel.hoplite.PropertySource
import com.sksamuel.hoplite.sources.EnvironmentVariablesPropertySource
import org.slf4j.LoggerFactory

data class IngressConfig(
    val traefikV3CrdSupport: Boolean = false,
)

data class Config(
    val imagePullSecrets: List<String> = emptyList(),
    val ingress: IngressConfig = IngressConfig(),
)

private val logger = LoggerFactory.getLogger("Config")

fun loadConfig(vararg resources: PropertySource): Config {
    logger.trace("Loading config...")
    val configLoader =
        ConfigLoaderBuilder
            .default()
            .addPropertySources(
                listOf(
                    EnvironmentVariablesPropertySource(
                        useUnderscoresAsSeparator = true,
                        allowUppercaseNames = true,
                        prefix = "FLAISERATOR",
                    ),
                ) + resources,
            ).build()
    return configLoader.loadConfigOrThrow<Config>().also { logger.trace("Loaded config: {}", it) }
}

fun defaultConfig() = loadConfig(PropertySource.resource("/application.yaml", optional = true))
