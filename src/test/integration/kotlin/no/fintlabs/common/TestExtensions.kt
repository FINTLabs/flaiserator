package no.fintlabs.common

import com.onepassword.v1.OnePasswordItem
import io.javaoperatorsdk.operator.api.reconciler.Reconciler
import no.fintlabs.Utils.createKoinTestExtension
import no.fintlabs.extensions.KubernetesOperatorExtension
import no.fintlabs.v1alpha1.KafkaUserAndAcl
import no.fintlabs.v1alpha1.PGUser
import org.koin.core.qualifier.named
import org.koin.dsl.module

val testModule = module {
  single<Reconciler<*>>(named("test-reconciler")) { TestReconciler() }
  single { TestConfigDR() }
  single { KafkaDR<FlaisTestResource>() }
  single { PostgresUserDR<FlaisTestResource>() }
  single { OnePasswordDR<FlaisTestResource>() }
}

fun createKoinTestExtension() = createKoinTestExtension(testModule)

fun createKubernetesOperatorExtension() =
    KubernetesOperatorExtension.create(
        listOf(
            FlaisTestResource::class.java,
            KafkaUserAndAcl::class.java,
            PGUser::class.java,
            OnePasswordItem::class.java,
        )
    )
