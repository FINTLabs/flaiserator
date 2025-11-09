package no.fintlabs

import io.fabric8.kubernetes.api.model.HasMetadata
import no.fintlabs.common.api.v1alpha1.FlaisResource
import no.fintlabs.common.api.v1alpha1.FlaisResourceState
import no.fintlabs.extensions.KubernetesOperatorContext
import org.awaitility.core.ConditionFactory
import org.awaitility.kotlin.await
import org.awaitility.kotlin.withPollDelay
import org.awaitility.kotlin.withPollInterval
import org.koin.core.module.Module
import org.koin.test.junit5.KoinTestExtension
import java.time.Duration

object Utils {
  inline fun <reified P : FlaisResource<*>, reified T : HasMetadata> KubernetesOperatorContext.createAndGetResource(
    source: P,
    nameSelector: (P) -> String = { it.metadata.name },
  ): T? {
    create(source)
    waitUntilIsDeployed(source)
    return get<T>(nameSelector(source))
  }

  inline fun <reified P : FlaisResource<*>, reified T : HasMetadata> KubernetesOperatorContext.updateAndGetResource(
    source: P,
    nameSelector: (P) -> String = { it.metadata.name },
  ): T? {
    update(source)
    waitUntilIsDeployed(source)
    return get<T>(nameSelector(source))
  }

  inline fun <reified T : FlaisResource<*>> KubernetesOperatorContext.waitUntilIsDeployed(source: T) {
    waitUntil<T>(
      source.metadata.name,
    ) {
      it.status?.state == FlaisResourceState.DEPLOYED &&
              it.status?.observedGeneration == it.metadata.generation
    }
  }

  inline fun <reified T : HasMetadata> KubernetesOperatorContext.waitUntil(
    resourceName: String,
    timeout: Duration = Duration.ofMinutes(1),
    pollInterval: Duration = Duration.ofMillis(50),
    pollDelay: Duration? = null,
    crossinline condition: (T) -> Boolean,
  ) {
    await.withOptionalPollDelay(pollDelay).withPollInterval(pollInterval).atMost(timeout).until() {
      get<T>(resourceName)?.let { condition(it) } ?: false
    }
  }

  infix fun ConditionFactory.withOptionalPollDelay(delay: Duration?): ConditionFactory =
    delay?.let { withPollDelay(it) } ?: this

  fun createKoinTestExtension(vararg additionalModules: Module) =
    KoinTestExtension.create {
      allowOverride(true)
      modules(baseModule)
      modules(additionalModules.toList())
    }
}



