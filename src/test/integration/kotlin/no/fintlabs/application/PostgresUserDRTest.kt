package no.fintlabs.application

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import no.fintlabs.application.Utils.createAndGetResource
import no.fintlabs.application.Utils.createKoinTestExtension
import no.fintlabs.application.Utils.createKubernetesOperatorExtension
import no.fintlabs.application.Utils.createTestFlaisApplication
import no.fintlabs.application.api.v1alpha1.Database
import no.fintlabs.application.api.v1alpha1.FlaisApplication
import no.fintlabs.extensions.KubernetesOperatorContext
import no.fintlabs.v1alpha1.PGUser
import org.junit.jupiter.api.extension.RegisterExtension

class PostgresUserDRTest {
  // region General
  @Test
  fun `should create PGUser`(context: KubernetesOperatorContext) {
    val flaisApplication =
        createTestFlaisApplication().apply { spec = spec.copy(database = Database("test-db")) }

    val pgUser = context.createAndGetPGUser(flaisApplication)
    assertNotNull(pgUser)
    assertEquals("${flaisApplication.metadata.name}-db", pgUser.metadata.name)
    assertEquals("test-db", pgUser.spec.database)
  }

  @Test
  fun `should not create PGUser since database is not set`(context: KubernetesOperatorContext) {
    val flaisApplication =
        createTestFlaisApplication().apply { spec = spec.copy(database = Database(null)) }

    val pgUser = context.createAndGetPGUser(flaisApplication)
    assertNull(pgUser)
  }

  // endregion

  private fun KubernetesOperatorContext.createAndGetPGUser(app: FlaisApplication) =
      createAndGetResource<PGUser>(app) { "${it.metadata.name}-db" }

  companion object {
    @RegisterExtension val koinTestExtension = createKoinTestExtension()

    @RegisterExtension val kubernetesOperatorExtension = createKubernetesOperatorExtension()
  }
}
