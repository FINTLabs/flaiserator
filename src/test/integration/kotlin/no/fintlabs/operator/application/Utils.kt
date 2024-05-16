package no.fintlabs.operator.application

import io.fabric8.kubernetes.api.model.HasMetadata
import io.fabric8.kubernetes.api.model.ObjectMeta
import no.fintlabs.operator.KubernetesOperatorContext
import no.fintlabs.operator.application.api.FlaisApplicationCrd
import no.fintlabs.operator.application.api.FlaisApplicationSpec
import no.fintlabs.operator.application.api.FlaisApplicationState
import org.awaitility.kotlin.atMost
import org.awaitility.kotlin.await
import org.awaitility.kotlin.until
import java.time.Duration


object Utils {
    inline fun <reified T : HasMetadata> KubernetesOperatorContext.createAndGetResource(app: FlaisApplicationCrd): T {
        create(app)
        await atMost Duration.ofSeconds(10) until {
            get<FlaisApplicationCrd>(app.metadata.name)?.status?.state == FlaisApplicationState.DEPLOYED
        }
        return get<T>(app.metadata.name) ?: error("${T::class.simpleName} not found")
    }

    fun createTestFlaisApplication(): FlaisApplicationCrd {
        return FlaisApplicationCrd().apply {
            metadata = ObjectMeta().apply {
                name = "test"

                labels = mapOf(
                    "fintlabs.no/team" to "test",
                    "fintlabs.no/org-id" to "test.org",
                )
            }
            spec = FlaisApplicationSpec(
                orgId = "test.org",
                image = "test-image"
            )
        }
    }
}