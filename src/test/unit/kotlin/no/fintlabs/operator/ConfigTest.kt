package no.fintlabs.operator

import com.sksamuel.hoplite.PropertySource
import no.fintlabs.defaultConfig
import no.fintlabs.loadConfig
import kotlin.test.Test
import kotlin.test.assertEquals

class ConfigTest {
    @Test
    fun `test default config`() {
        val config = defaultConfig()
        assert(config.imagePullSecrets.isEmpty())
    }

    @Test
    fun `test load config`() {
        val config = loadConfig(PropertySource.resource("/config/test-1.yaml"))
        assertEquals("secret1", config.imagePullSecrets[0])
    }
}