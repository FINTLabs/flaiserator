package no.fintlabs.operator

import com.onepassword.v1.OnePasswordItem
import junit.framework.TestCase.assertNull
import no.fintlabs.extensions.KubernetesOperatorContext
import no.fintlabs.operator.Utils.createAndGetResource
import no.fintlabs.operator.Utils.createKoinTestExtension
import no.fintlabs.operator.Utils.createKubernetesOperatorExtension
import no.fintlabs.operator.Utils.createTestFlaisApplication
import no.fintlabs.operator.api.v1alpha1.FlaisApplicationCrd
import no.fintlabs.operator.api.v1alpha1.OnePassword
import org.junit.jupiter.api.extension.RegisterExtension
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class OnePasswordDRTest {
    //region General
    @Test
    fun `should create OnePasswordItem`(context: KubernetesOperatorContext) {
        val flaisApplication = createTestFlaisApplication().apply {
            spec = spec.copy(onePassword = OnePassword("test-item"))
        }

        val onePasswordItem = context.createAndGetOnePasswordItem(flaisApplication)
        assertNotNull(onePasswordItem)
        assertEquals("${flaisApplication.metadata.name}-op", onePasswordItem.metadata.name)
        assertEquals("test-item", onePasswordItem.spec.itemPath)
    }

    @Test
    fun `should not create OnePasswordItem since onePassword is not set`(context: KubernetesOperatorContext) {
        val flaisApplication = createTestFlaisApplication()

        val onePasswordItem = context.createAndGetOnePasswordItem(flaisApplication)
        assertNull(onePasswordItem)
    }

    @Test
    fun `should not create OnePasswordItem since itemPath is not set`(context: KubernetesOperatorContext) {
        val flaisApplication = createTestFlaisApplication().apply {
            spec = spec.copy(onePassword = OnePassword())
        }

        val onePasswordItem = context.createAndGetOnePasswordItem(flaisApplication)
        assertNull(onePasswordItem)
    }
    //endregion

    private fun KubernetesOperatorContext.createAndGetOnePasswordItem(app: FlaisApplicationCrd) =
        createAndGetResource<OnePasswordItem>(app) { "${it.metadata.name}-op" }

    companion object {
        @RegisterExtension
        val koinTestExtension = createKoinTestExtension()

        @RegisterExtension
        val kubernetesOperatorExtension = createKubernetesOperatorExtension()
    }
}