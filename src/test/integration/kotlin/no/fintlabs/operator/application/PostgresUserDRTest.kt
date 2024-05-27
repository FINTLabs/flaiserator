package no.fintlabs.operator.application

import no.fintlabs.extensions.KubernetesOperatorContext
import no.fintlabs.operator.application.Utils.createAndGetResource
import no.fintlabs.operator.application.Utils.createKoinTestExtension
import no.fintlabs.operator.application.Utils.createKubernetesOperatorExtension
import no.fintlabs.operator.application.Utils.createTestFlaisApplication
import no.fintlabs.operator.application.api.Database
import no.fintlabs.operator.application.api.FlaisApplicationCrd
import no.fintlabs.v1alpha1.PGUser
import org.junit.jupiter.api.extension.RegisterExtension
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull


class PostgresUserDRTest {
    //region General
    @Test
    fun `should create PGUser`(context: KubernetesOperatorContext) {
        val flaisApplication = createTestFlaisApplication().apply {
            spec = spec.copy(database = Database(true, "test-db"))
        }

        val pgUser = context.createAndGetPGUser(flaisApplication)
        assertNotNull(pgUser)
        assertEquals("${flaisApplication.metadata.name}-db", pgUser.metadata.name)
        assertEquals("test-db", pgUser.spec.database)

    }

    @Test
    fun `should not create PGUser since enabled is false`(context: KubernetesOperatorContext) {
        val flaisApplication = createTestFlaisApplication().apply {
            spec = spec.copy(database = Database(false, "test-db"))
        }

        val pgUser = context.createAndGetPGUser(flaisApplication)
        assertNull(pgUser)
    }

    @Test
    fun `should not create PGUser since database is not set`(context: KubernetesOperatorContext) {
        val flaisApplication = createTestFlaisApplication().apply {
            spec = spec.copy(database = Database(true))
        }

        val pgUser = context.createAndGetPGUser(flaisApplication)
        assertNull(pgUser)
    }
    //endregion

    private fun KubernetesOperatorContext.createAndGetPGUser(app: FlaisApplicationCrd) =
        createAndGetResource<PGUser>(app) { "${it.metadata.name}-db" }

    companion object {
        @RegisterExtension
        val koinTestExtension = createKoinTestExtension()

        @RegisterExtension
        val kubernetesOperatorExtension = createKubernetesOperatorExtension()
    }
}