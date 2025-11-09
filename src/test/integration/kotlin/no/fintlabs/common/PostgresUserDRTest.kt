package no.fintlabs.common

import no.fintlabs.common.Utils.createAndGetResource
import no.fintlabs.common.Utils.createKoinTestExtension
import no.fintlabs.common.Utils.createKubernetesOperatorExtension
import no.fintlabs.common.Utils.createTestResource
import no.fintlabs.common.api.v1alpha1.Database
import no.fintlabs.extensions.KubernetesOperatorContext
import no.fintlabs.v1alpha1.PGUser
import org.junit.jupiter.api.extension.RegisterExtension
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class PostgresUserDRTest {
  // region General
  @Test
  fun `should create PGUser`(context: KubernetesOperatorContext) {
    val testResource =
      createTestResource().apply { spec = spec.copy(database = Database("test-db")) }

    val pgUser = context.createAndGetPGUser(testResource)
    assertNotNull(pgUser)
    assertEquals("${testResource.metadata.name}-db", pgUser.metadata.name)
    assertEquals("test-db", pgUser.spec.database)
  }

  @Test
  fun `should not create PGUser since database is not set`(context: KubernetesOperatorContext) {
    val testResource =
      createTestResource().apply { spec = spec.copy(database = Database(null)) }

    val pgUser = context.createAndGetPGUser(testResource)
    assertNull(pgUser)
  }

  // endregion

  private fun KubernetesOperatorContext.createAndGetPGUser(resource: FlaisTestResource) =
      createAndGetResource<PGUser>(resource) { "${it.metadata.name}-db" }

  companion object {
    @RegisterExtension val koinTestExtension = createKoinTestExtension()

    @RegisterExtension val kubernetesOperatorExtension = createKubernetesOperatorExtension()
  }
}
