package no.fintlabs.job

import com.onepassword.v1.OnePasswordItem
import io.fabric8.kubernetes.api.model.HasMetadata
import io.fabric8.kubernetes.api.model.ObjectMeta
import no.fintlabs.Utils.createAndGetResource
import no.fintlabs.Utils.createKoinTestExtension
import no.fintlabs.extensions.KubernetesOperatorContext
import no.fintlabs.extensions.KubernetesOperatorExtension
import no.fintlabs.job.api.v1alpha1.FlaisJob
import no.fintlabs.job.api.v1alpha1.FlaisJobSpec
import no.fintlabs.v1alpha1.KafkaUserAndAcl
import no.fintlabs.v1alpha1.PGUser
import org.koin.core.module.Module

object Utils {
  fun createTestFlaisJob() =
      FlaisJob().apply {
        metadata =
            ObjectMeta().apply {
              name = "test"

              labels =
                  mutableMapOf(
                      "fintlabs.no/team" to "test",
                      "fintlabs.no/org-id" to "test.org",
                  )
            }
        spec = FlaisJobSpec(orgId = "test.org", image = "hello-world", schedule = "0 0 1 1 *")
      }

  inline fun <reified T : HasMetadata> KubernetesOperatorContext.createAndGetResource(
      source: FlaisJob,
      nameSelector: (FlaisJob) -> String = { it.metadata.name },
  ): T? = createAndGetResource<FlaisJob, T>(source, nameSelector)

  fun createJobKoinTestExtension(vararg additionalModules: Module) =
      createKoinTestExtension(jobReconcilerModule(), *additionalModules)

  fun createJobKubernetesOperatorExtension() =
      KubernetesOperatorExtension.create(
          listOf(
              FlaisJob::class.java,
              PGUser::class.java,
              KafkaUserAndAcl::class.java,
              OnePasswordItem::class.java,
          )
      )
}
