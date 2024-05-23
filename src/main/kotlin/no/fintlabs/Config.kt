package no.fintlabs

import com.sksamuel.hoplite.ConfigLoaderBuilder
import com.sksamuel.hoplite.PropertySource

data class Config (
    val imagePullSecrets: List<String>
)

fun loadConfig(vararg resources: String): Config {
    val environmentPropertySource = PropertySource.environment()
    val resourcePropertySources = resources.map { PropertySource.resource(it) }

    val configLoader = ConfigLoaderBuilder.default()
        .addPropertySources(listOf(environmentPropertySource) + resourcePropertySources)
        .build()

    return configLoader.loadConfigOrThrow<Config>()
}