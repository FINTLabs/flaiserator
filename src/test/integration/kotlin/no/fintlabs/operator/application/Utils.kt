package no.fintlabs.operator.application

import com.onepassword.v1.OnePasswordItem
import io.fabric8.kubernetes.api.model.HasMetadata
import io.fabric8.kubernetes.api.model.ObjectMeta
import no.fintlabs.baseModule
import no.fintlabs.operator.KubernetesOperatorContext
import no.fintlabs.operator.KubernetesOperatorExtension
import no.fintlabs.operator.application.api.FlaisApplicationCrd
import no.fintlabs.operator.application.api.FlaisApplicationSpec
import no.fintlabs.operator.application.api.FlaisApplicationState
import no.fintlabs.v1alpha1.KafkaUserAndAcl
import no.fintlabs.v1alpha1.PGUser
import org.awaitility.kotlin.atMost
import org.awaitility.kotlin.await
import org.awaitility.kotlin.until
import org.koin.test.junit5.KoinTestExtension
import us.containo.traefik.v1alpha1.IngressRoute
import java.time.Duration


object Utils {
    inline fun <reified T : HasMetadata> KubernetesOperatorContext.createAndGetResource(app: FlaisApplicationCrd): T? {
        create(app)
        await atMost Duration.ofSeconds(10) until {
            get<FlaisApplicationCrd>(app.metadata.name)?.status?.state == FlaisApplicationState.DEPLOYED
        }
        return get<T>(app.metadata.name)
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

    fun createKubernetesOperatorExtension() = KubernetesOperatorExtension.create(
        listOf(FlaisApplicationCrd::class.java, IngressRoute::class.java, PGUser::class.java, KafkaUserAndAcl::class.java, OnePasswordItem::class.java)
    )

    fun createKoinTestExtension() = KoinTestExtension.create {
        modules(baseModule, applicationReconcilerModule())
    }
}