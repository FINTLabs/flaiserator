package no.fintlabs

import com.sksamuel.hoplite.ConfigLoaderBuilder
import com.sksamuel.hoplite.PropertySource
import com.sksamuel.hoplite.sources.EnvironmentVariablesPropertySource
import org.slf4j.LoggerFactory

data class IngressConfig(
    val traefikV3CrdSupport: Boolean = false,
)

data class ObservabilityConfig(
    val otel: OtelConfig = OtelConfig(),
)

data class OtelConfig(
    val enabled: Boolean = false,
    val autoInstrumentation: OtelAutoInstrumentationConfig = OtelAutoInstrumentationConfig(),
    val instrumentation: OtelInstrumentationConfig? = null,
)

data class OtelInstrumentationConfig(
    val collectorEndpoint: String,
    val collectorProtocol: String,
    val collectorInsecure: Boolean = false,
)

data class OtelAutoInstrumentationConfig(
    val ebpfEnabled: Boolean = false,
    val sdkInjectionEnabled: Boolean = false,
    val instrumentationConfig: String = "flais-system/apps",
)

data class Config(
    val imagePullSecrets: List<String> = emptyList(),
    val ingress: IngressConfig = IngressConfig(),
    val observability: ObservabilityConfig = ObservabilityConfig(),
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
                        prefix = "FLAISERATOR_",
                    ),
                ) + resources,
            ).build()
    return configLoader.loadConfigOrThrow<Config>().also { logger.trace("Loaded config: {}", it) }
}

fun defaultConfig() = loadConfig(PropertySource.resource("/application.yaml", optional = true))
