package no.fintlabs.common

import io.fabric8.kubernetes.api.model.ObjectMeta
import kotlin.test.Test
import kotlin.test.assertNotEquals
import no.fintlabs.common.api.v1alpha1.resourceHash

class FlaisResourceHashTest {
  @Test
  fun `hash changes when labels change`() {
    val resource =
        TestResource().apply {
          metadata = ObjectMeta().apply { labels = mutableMapOf("fintlabs.no/team" to "alpha") }
          spec = TestSpec()
        }

    val originalHash = resource.resourceHash()

    resource.metadata.labels["fintlabs.no/team"] = "beta"

    assertNotEquals(originalHash, resource.resourceHash())
  }

  @Test
  fun `hash changes when change cause annotation changes`() {
    val resource =
        TestResource().apply {
          metadata =
              ObjectMeta().apply {
                annotations = mutableMapOf("kubernetes.io/change-cause" to "initial deployment")
              }
          spec = TestSpec()
        }

    val originalHash = resource.resourceHash()

    resource.metadata.annotations["kubernetes.io/change-cause"] = "manual update"

    assertNotEquals(originalHash, resource.resourceHash())
  }

  @Test
  fun `hash changes when spec changes`() {
    val resource =
        TestResource().apply {
          metadata = ObjectMeta()
          spec = TestSpec(image = "image-a")
        }

    val originalHash = resource.resourceHash()

    resource.spec = TestSpec(image = "image-b")

    assertNotEquals(originalHash, resource.resourceHash())
  }
}
