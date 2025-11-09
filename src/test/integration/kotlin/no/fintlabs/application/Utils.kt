package no.fintlabs.application

import com.coreos.monitoring.v1.PodMonitor
import com.onepassword.v1.OnePasswordItem
import io.fabric8.kubernetes.api.model.HasMetadata
import io.fabric8.kubernetes.api.model.ObjectMeta
import no.fintlabs.Utils.createAndGetResource
import no.fintlabs.Utils.createKoinTestExtension
import no.fintlabs.application.api.v1alpha1.FlaisApplication
import no.fintlabs.application.api.v1alpha1.FlaisApplicationSpec
import no.fintlabs.extensions.KubernetesOperatorContext
import no.fintlabs.extensions.KubernetesOperatorExtension
import no.fintlabs.v1alpha1.KafkaUserAndAcl
import no.fintlabs.v1alpha1.PGUser
import org.koin.core.module.Module
import us.containo.traefik.v1alpha1.IngressRoute

object Utils {
  fun createTestFlaisApplication(): FlaisApplication {
    return FlaisApplication().apply {
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

  inline fun <reified T : HasMetadata> KubernetesOperatorContext.createAndGetResource(
      source: FlaisApplication,
      nameSelector: (FlaisApplication) -> String = { it.metadata.name },
  ): T? = createAndGetResource<FlaisApplication, T>(source, nameSelector)

  fun createApplicationKoinTestExtension(vararg additionalModules: Module) =
      createKoinTestExtension(applicationReconcilerModule(), *additionalModules)

  fun createApplicationKubernetesOperatorExtension() =
      KubernetesOperatorExtension.create(
          listOf(
              FlaisApplication::class.java,
              IngressRoute::class.java,
              PGUser::class.java,
              KafkaUserAndAcl::class.java,
              OnePasswordItem::class.java,
              PodMonitor::class.java,
          )
      )
}
