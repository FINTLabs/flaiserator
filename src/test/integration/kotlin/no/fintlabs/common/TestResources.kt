package no.fintlabs.common

import io.fabric8.kubernetes.api.model.HasMetadata
import io.fabric8.kubernetes.api.model.ObjectMeta
import no.fintlabs.Utils.createAndGetResource
import no.fintlabs.extensions.KubernetesOperatorContext

fun createTestResource() =
    FlaisTestResource().apply {
      metadata =
          ObjectMeta().apply {
            name = "test"

            labels =
                mutableMapOf(
                    "fintlabs.no/team" to "test",
                    "fintlabs.no/org-id" to "test.org",
                )
          }
      spec = FlaisTestResourceSpec()
    }

inline fun <reified T : HasMetadata> KubernetesOperatorContext.createAndGetResource(
    source: FlaisTestResource,
    nameSelector: (FlaisTestResource) -> String = { it.metadata.name },
): T? = createAndGetResource<FlaisTestResource, T>(source, nameSelector)
