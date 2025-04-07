package no.fintlabs

import no.fintlabs.extensions.KubernetesOperatorContext
import no.fintlabs.application.Utils.createKoinTestExtension
import no.fintlabs.application.Utils.createKubernetesOperatorExtension
import org.http4k.core.HttpHandler
import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.core.Status
import org.junit.jupiter.api.extension.RegisterExtension
import org.koin.test.KoinTest
import org.koin.test.inject
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals

class HttpTests : KoinTest {
    @Test
    fun `should return metrics`() {
        val client: HttpHandler by inject()

        val response = client(Request(Method.GET, "/metrics"))
        assertEquals(Status.OK, response.status)
        assertContains(response.bodyString(), "jvm_memory_used_bytes")
    }

    @Test
    fun `should return health ok`() {
        val client: HttpHandler by inject()

        val response = client(Request(Method.GET, "/health"))
        assertEquals(Status.OK, response.status)
    }

    @Test
    fun `should return health fail`(context: KubernetesOperatorContext) {
        val client: HttpHandler by inject()

        context.operator.stop()
        val response = client(Request(Method.GET, "/health"))
        assertEquals(Status.SERVICE_UNAVAILABLE, response.status)
    }

    @Test
    fun `should return readiness ok`() {
        val client: HttpHandler by inject()

        val response = client(Request(Method.GET, "/ready"))
        assertEquals(Status.OK, response.status)
    }

    @Test
    fun `should return readiness fail`(context: KubernetesOperatorContext) {
        val client: HttpHandler by inject()

        context.operator.stop()
        val response = client(Request(Method.GET, "/ready"))
        assertEquals(Status.SERVICE_UNAVAILABLE, response.status)
    }

    companion object {
        @RegisterExtension
        val koinTestExtension = createKoinTestExtension()

        @RegisterExtension
        val kubernetesOperatorExtension = createKubernetesOperatorExtension()
    }
}