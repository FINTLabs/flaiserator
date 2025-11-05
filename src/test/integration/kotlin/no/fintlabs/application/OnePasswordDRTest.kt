package no.fintlabs.application

import com.onepassword.v1.OnePasswordItem
import junit.framework.TestCase.assertNull
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import no.fintlabs.application.Utils.createAndGetResource
import no.fintlabs.application.Utils.createKoinTestExtension
import no.fintlabs.application.Utils.createKubernetesOperatorExtension
import no.fintlabs.application.Utils.createTestFlaisApplication
import no.fintlabs.application.api.v1alpha1.FlaisApplication
import no.fintlabs.common.api.v1alpha1.OnePassword
import no.fintlabs.extensions.KubernetesOperatorContext
import org.junit.jupiter.api.extension.RegisterExtension

class OnePasswordDRTest {
  // region General
  @Test
  fun `should create OnePasswordItem`(context: KubernetesOperatorContext) {
    val flaisApplication =
        createTestFlaisApplication().apply {
          spec = spec.copy(onePassword = OnePassword("test-item"))
        }

    val onePasswordItem = context.createAndGetOnePasswordItem(flaisApplication)
    assertNotNull(onePasswordItem)
    assertEquals("${flaisApplication.metadata.name}-op", onePasswordItem.metadata.name)
    assertEquals("test-item", onePasswordItem.spec.itemPath)
  }

  @Test
  fun `should not create OnePasswordItem since onePassword is not set`(
      context: KubernetesOperatorContext
  ) {
    val flaisApplication = createTestFlaisApplication()

    val onePasswordItem = context.createAndGetOnePasswordItem(flaisApplication)
    assertNull(onePasswordItem)
  }

  @Test
  fun `should not create OnePasswordItem since itemPath is not set`(
      context: KubernetesOperatorContext
  ) {
    val flaisApplication =
        createTestFlaisApplication().apply { spec = spec.copy(onePassword = OnePassword()) }

    val onePasswordItem = context.createAndGetOnePasswordItem(flaisApplication)
    assertNull(onePasswordItem)
  }

  // endregion

  private fun KubernetesOperatorContext.createAndGetOnePasswordItem(app: FlaisApplication) =
      createAndGetResource<OnePasswordItem>(app) { "${it.metadata.name}-op" }

  companion object {
    @RegisterExtension val koinTestExtension = createKoinTestExtension()

    @RegisterExtension val kubernetesOperatorExtension = createKubernetesOperatorExtension()
  }
}
