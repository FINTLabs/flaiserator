package no.fintlabs

import com.sksamuel.hoplite.PropertySource
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class ConfigTest {
    @Test
    fun `test default config`() {
        val config = defaultConfig()
        assert(config.imagePullSecrets.isEmpty())
    }

    @Test
    fun `test load config`() {
        val config = loadConfig(PropertySource.Companion.resource("/config/test-1.yaml"))
        assertEquals("secret1", config.imagePullSecrets[0])
        assertEquals(true, config.observability.otel.enabled)
        assertEquals(true, config.observability.otel.autoInstrumentation.ebpfEnabled)
        assertEquals(true, config.observability.otel.autoInstrumentation.sdkInjectionEnabled)
        assertEquals("observability/apps", config.observability.otel.autoInstrumentation.instrumentationConfig)
        assertEquals(
            "http://otel-collector:4318",
            config.observability.otel.instrumentation
                ?.collectorEndpoint,
        )
        assertEquals(
            "http/protobuf",
            config.observability.otel.instrumentation
                ?.collectorProtocol,
        )
        assertEquals(
            true,
            config.observability.otel.instrumentation
                ?.collectorInsecure,
        )
    }

    @Test
    fun `should fail when otel is enabled without sdk injection or collector`() {
        val yaml =
            """
            observability:
              otel:
                enabled: true
                autoInstrumentation:
                  sdkInjectionEnabled: false
            """.trimIndent()

        assertFailsWith<IllegalStateException> {
            loadConfig(PropertySource.string(yaml, "yaml"))
        }
    }

    @Test
    fun `should load when otel is enabled with collector configured`() {
        val yaml =
            """
            observability:
              otel:
                enabled: true
                autoInstrumentation:
                  sdkInjectionEnabled: false
                instrumentation:
                  collectorEndpoint: http://otel-collector:4318
                  collectorProtocol: http/protobuf
            """.trimIndent()

        val config = loadConfig(PropertySource.string(yaml, "yaml"))
        assertEquals(
            "http://otel-collector:4318",
            config.observability.otel.instrumentation
                ?.collectorEndpoint,
        )
    }

    @Test
    fun `should load when otel is enabled with sdk injection`() {
        val yaml =
            """
            observability:
              otel:
                enabled: true
                autoInstrumentation:
                  sdkInjectionEnabled: true
            """.trimIndent()

        val config = loadConfig(PropertySource.string(yaml, "yaml"))
        assertEquals(true, config.observability.otel.autoInstrumentation.sdkInjectionEnabled)
    }
}
