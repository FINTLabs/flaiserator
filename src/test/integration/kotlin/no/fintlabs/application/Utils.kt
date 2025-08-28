package no.fintlabs.application

import com.coreos.monitoring.v1.PodMonitor
import com.onepassword.v1.OnePasswordItem
import io.fabric8.kubernetes.api.model.HasMetadata
import io.fabric8.kubernetes.api.model.ObjectMeta
import java.time.Duration
import no.fintlabs.application.api.v1alpha1.FlaisApplicationCrd
import no.fintlabs.application.api.v1alpha1.FlaisApplicationSpec
import no.fintlabs.application.api.v1alpha1.FlaisApplicationState
import no.fintlabs.baseModule
import no.fintlabs.extensions.KubernetesOperatorContext
import no.fintlabs.extensions.KubernetesOperatorExtension
import no.fintlabs.v1alpha1.KafkaUserAndAcl
import no.fintlabs.v1alpha1.PGUser
import org.awaitility.core.ConditionFactory
import org.awaitility.kotlin.await
import org.awaitility.kotlin.withPollDelay
import org.awaitility.kotlin.withPollInterval
import org.koin.core.module.Module
import org.koin.test.junit5.KoinTestExtension
import us.containo.traefik.v1alpha1.IngressRoute

object Utils {
  inline fun <reified T : HasMetadata> KubernetesOperatorContext.createAndGetResource(
      app: FlaisApplicationCrd,
      nameSelector: (FlaisApplicationCrd) -> String = { it.metadata.name },
  ): T? {
    create(app)
    waitUntilIsDeployed(app)
    return get<T>(nameSelector(app))
  }

  inline fun <reified T : HasMetadata> KubernetesOperatorContext.updateAndGetResource(
      app: FlaisApplicationCrd,
      nameSelector: (FlaisApplicationCrd) -> String = { it.metadata.name },
  ): T? {
    update(app)
    waitUntilIsDeployed(app)
    return get<T>(nameSelector(app))
  }

  fun KubernetesOperatorContext.waitUntilIsDeployed(app: FlaisApplicationCrd) {
    waitUntil<FlaisApplicationCrd>(
        app.metadata.name,
    ) {
      it.status?.state == FlaisApplicationState.DEPLOYED &&
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

  fun createTestFlaisApplication(): FlaisApplicationCrd {
    return FlaisApplicationCrd().apply {
      metadata =
          ObjectMeta().apply {
            name = "test"

            labels =
                mutableMapOf(
                    "fintlabs.no/team" to "test",
                    "fintlabs.no/org-id" to "test.org",
                )
          }
      spec = FlaisApplicationSpec(orgId = "test.org", image = "hello-world")
    }
  }

  fun createKubernetesOperatorExtension() =
      KubernetesOperatorExtension.create(
          listOf(
              FlaisApplicationCrd::class.java,
              IngressRoute::class.java,
              PGUser::class.java,
              KafkaUserAndAcl::class.java,
              OnePasswordItem::class.java,
              PodMonitor::class.java,
          )
      )

  fun createKoinTestExtension(additionalModule: Module? = null) =
      KoinTestExtension.create {
        allowOverride(true)
        modules(baseModule, applicationReconcilerModule())
        additionalModule?.let { modules(it) }
      }
}
