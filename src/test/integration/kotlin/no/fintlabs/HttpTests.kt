package no.fintlabs

import no.fintlabs.operator.Utils.createKoinTestExtension
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

    companion object {
        @RegisterExtension
        val koinTestExtension = createKoinTestExtension()
    }
}