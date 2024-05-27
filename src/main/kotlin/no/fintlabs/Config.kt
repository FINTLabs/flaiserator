package no.fintlabs

import com.sksamuel.hoplite.ConfigLoaderBuilder
import com.sksamuel.hoplite.PropertySource
import org.slf4j.LoggerFactory

data class Config (
    val imagePullSecrets: List<String> = emptyList()
)
private val logger = LoggerFactory.getLogger("Config")

fun loadConfig(vararg resources: PropertySource): Config {
    logger.trace("Loading config...")
    val configLoader = ConfigLoaderBuilder.default()
        .addPropertySources(listOf(PropertySource.environment()) + resources)
        .build()
    return configLoader.loadConfigOrThrow<Config>().also {
        logger.trace("Loaded config: {}", it)
    }
}

fun defaultConfig() = loadConfig(PropertySource.resource("/application.yaml", optional = true))

