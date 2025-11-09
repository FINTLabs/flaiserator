package no.fintlabs

import com.sksamuel.hoplite.PropertySource
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
    val config = loadConfig(PropertySource.Companion.resource("/config/test-1.yaml"))
    assertEquals("secret1", config.imagePullSecrets[0])
  }
}
