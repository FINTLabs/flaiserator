package no.fintlabs.common

import com.onepassword.v1.OnePasswordItem
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import no.fintlabs.common.Utils.createAndGetResource
import no.fintlabs.common.Utils.createKoinTestExtension
import no.fintlabs.common.Utils.createKubernetesOperatorExtension
import no.fintlabs.common.Utils.createTestResource
import no.fintlabs.common.api.v1alpha1.OnePassword
import no.fintlabs.extensions.KubernetesOperatorContext
import org.junit.jupiter.api.extension.RegisterExtension
import kotlin.test.assertNull

class OnePasswordDRTest {
  // region General
  @Test
  fun `should create OnePasswordItem`(context: KubernetesOperatorContext) {
    val testResource =
        createTestResource().apply { spec = spec.copy(onePassword = OnePassword("test-item")) }

    val onePasswordItem = context.createAndGetOnePasswordItem(testResource)
    assertNotNull(onePasswordItem)
    assertEquals("${testResource.metadata.name}-op", onePasswordItem.metadata.name)
    assertEquals("test-item", onePasswordItem.spec.itemPath)
  }

  @Test
  fun `should not create OnePasswordItem since onePassword is not set`(
      context: KubernetesOperatorContext
  ) {
    val testResource = createTestResource()

    val onePasswordItem = context.createAndGetOnePasswordItem(testResource)
    assertNull(onePasswordItem)
  }

  @Test
  fun `should not create OnePasswordItem since itemPath is not set`(
      context: KubernetesOperatorContext
  ) {
    val testResource = createTestResource().apply { spec = spec.copy(onePassword = OnePassword()) }

    val onePasswordItem = context.createAndGetOnePasswordItem(testResource)
    assertNull(onePasswordItem)
  }

  // endregion

  private fun KubernetesOperatorContext.createAndGetOnePasswordItem(resource: FlaisTestResource) =
      createAndGetResource<OnePasswordItem>(resource) { "${it.metadata.name}-op" }

  companion object {
    @RegisterExtension val koinTestExtension = createKoinTestExtension()

    @RegisterExtension val kubernetesOperatorExtension = createKubernetesOperatorExtension()
  }
}
