package no.fintlabs.common

import com.onepassword.v1.OnePasswordItem
import io.fabric8.kubernetes.api.model.HasMetadata
import io.fabric8.kubernetes.api.model.ObjectMeta
import io.javaoperatorsdk.operator.api.reconciler.Reconciler
import no.fintlabs.Utils.createAndGetResource
import org.koin.core.qualifier.named
import org.koin.dsl.module
import no.fintlabs.Utils.createKoinTestExtension
import no.fintlabs.application.api.v1alpha1.FlaisApplication
import no.fintlabs.extensions.KubernetesOperatorContext
import no.fintlabs.extensions.KubernetesOperatorExtension
import no.fintlabs.v1alpha1.KafkaUserAndAcl
import no.fintlabs.v1alpha1.PGUser

object Utils {
  fun createTestResource() = FlaisTestResource().apply {
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


  val testModule = module {
    single<Reconciler<*>>(named("test-reconciler")) { TestReconciler() }
    single { TestConfigDR() }
    single { KafkaDR<FlaisTestResource>() }
    single { PostgresUserDR<FlaisTestResource>() }
    single { OnePasswordDR<FlaisTestResource>() }
  }

  fun createKoinTestExtension() = createKoinTestExtension(testModule)

  fun createKubernetesOperatorExtension() = KubernetesOperatorExtension.create(
    listOf(
      FlaisTestResource::class.java,
      KafkaUserAndAcl::class.java,
      PGUser::class.java,
      OnePasswordItem::class.java
    )
  )
}
