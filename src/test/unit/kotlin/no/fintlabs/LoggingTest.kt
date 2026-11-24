package no.fintlabs

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.Logger
import ch.qos.logback.classic.LoggerContext
import ch.qos.logback.classic.joran.JoranConfigurator
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.classic.spi.LoggingEvent
import ch.qos.logback.core.ConsoleAppender
import ch.qos.logback.core.encoder.Encoder
import ch.qos.logback.core.status.Status
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class LoggingTest {
    private val traceContext =
        mapOf(
            "trace_id" to "0123456789abcdef0123456789abcdef",
            "span_id" to "0123456789abcdef",
            "correlationId" to "resource-update-123",
        )

    @Test
    fun `default JSON logs preserve trace context and encode exceptions on one line`() =
        withEncoder { encoder, context ->
            val message = "Reconciliation failed\nwith \"details\""
            val output =
                encoder
                    .encode(
                        event(context, message, traceContext, IllegalStateException("test failure")),
                    ).toString(Charsets.UTF_8)
            assertEquals(1, output.trimEnd().lines().size)
            val json = jacksonObjectMapper().readTree(output)
            assertEquals(message, json["message"].asText())
            assertEquals("INFO", json["level"].asText())
            assertEquals("logging-test", json["class"].asText())
            assertEquals("test-thread", json["thread"].asText())
            assertTrue(json["timestamp"].asText().isNotBlank())
            traceContext.forEach { (key, value) -> assertEquals(value, json[key].asText()) }
            assertTrue(json["stack_trace"].asText().contains("IllegalStateException: test failure"))
        }

    @Test
    fun `JSON logs outside a span omit trace fields and keep resource correlation`() =
        withEncoder { encoder, context ->
            val output = encoder.encode(event(context, "No active span", mapOf("correlationId" to "resource-update-456")))
            val json = jacksonObjectMapper().readTree(output)
            assertFalse(json.has("trace_id"))
            assertFalse(json.has("span_id"))
            assertEquals("resource-update-456", json["correlationId"].asText())
        }

    @Test
    fun `plain text logs include trace and span IDs`() =
        withEncoder("STDOUT") { encoder, context ->
            val output = encoder.encode(event(context, "Reconciliation started", traceContext)).toString(Charsets.UTF_8)
            assertTrue(output.contains("trace_id=${traceContext["trace_id"]}"))
            assertTrue(output.contains("span_id=${traceContext["span_id"]}"))
            assertTrue(output.contains("Reconciliation started"))
        }

    private fun event(
        context: LoggerContext,
        message: String,
        mdc: Map<String, String>,
        error: Throwable? = null,
    ): LoggingEvent =
        LoggingEvent(javaClass.name, context.getLogger("logging-test"), Level.INFO, message, error, null).apply {
            mdcPropertyMap = mdc
            threadName = "test-thread"
        }

    private fun withEncoder(
        appender: String? = null,
        verify: (Encoder<ILoggingEvent>, LoggerContext) -> Unit,
    ) {
        val context = LoggerContext()
        try {
            appender?.let { context.putProperty("LOGBACK_APPENDER", it) }
            JoranConfigurator().apply {
                this.context = context
                // Unit tests normally use their own logback.xml; verify the production configuration here.
                doConfigure(Path.of("src/main/resources/logback.xml").toFile())
            }
            val errors = context.statusManager.copyOfStatusList.filter { it.effectiveLevel >= Status.ERROR }
            assertTrue(errors.isEmpty(), "Logback configuration errors: $errors")
            @Suppress("UNCHECKED_CAST")
            val console =
                context
                    .getLogger(
                        Logger.ROOT_LOGGER_NAME,
                    ).iteratorForAppenders()
                    .asSequence()
                    .single() as ConsoleAppender<ILoggingEvent>
            assertTrue(console.isStarted)
            verify(console.encoder, context)
        } finally {
            context.stop()
        }
    }
}
