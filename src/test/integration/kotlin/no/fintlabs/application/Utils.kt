package no.fintlabs.application

import com.coreos.monitoring.v1.PodMonitor
import com.onepassword.v1.OnePasswordItem
import io.fabric8.kubernetes.api.model.HasMetadata
import io.fabric8.kubernetes.api.model.ObjectMeta
import no.fintlabs.baseModule
import no.fintlabs.extensions.KubernetesOperatorContext
import no.fintlabs.extensions.KubernetesOperatorExtension
import no.fintlabs.application.api.v1alpha1.FlaisApplicationCrd
import no.fintlabs.application.api.v1alpha1.FlaisApplicationSpec
import no.fintlabs.application.api.v1alpha1.FlaisApplicationState
import no.fintlabs.v1alpha1.KafkaUserAndAcl
import no.fintlabs.v1alpha1.PGUser
import org.awaitility.kotlin.atMost
import org.awaitility.kotlin.await
import org.awaitility.kotlin.until
import org.koin.core.module.Module
import org.koin.test.junit5.KoinTestExtension
import us.containo.traefik.v1alpha1.IngressRoute
import java.time.Duration


object Utils {
    inline fun <reified T : HasMetadata> KubernetesOperatorContext.createAndGetResource(app: FlaisApplicationCrd, nameSelector: (FlaisApplicationCrd) -> String = { it.metadata.name }): T? {
        create(app)
        waitUntilIsDeployed(app)
        return get<T>(nameSelector(app))
    }

    inline fun <reified T : HasMetadata> KubernetesOperatorContext.updateAndGetResource(app: FlaisApplicationCrd, nameSelector: (FlaisApplicationCrd) -> String = { it.metadata.name }): T? {
        update(app)
        waitUntilIsDeployed(app)
        return get<T>(nameSelector(app))
    }

    fun KubernetesOperatorContext.waitUntilIsDeployed(app: FlaisApplicationCrd) {
        waitUntil<FlaisApplicationCrd>(
            app.metadata.name,
        ) { it.status?.state == FlaisApplicationState.DEPLOYED }
    }

    inline fun <reified T : HasMetadata> KubernetesOperatorContext.waitUntil(resourceName: String, timeout: Duration = Duration.ofSeconds(1000), crossinline condition: (T) -> Boolean) {
        await atMost timeout until {
            get<T>(resourceName)?.let { condition(it) } ?: false
        }
    }



    fun createTestFlaisApplication(): FlaisApplicationCrd {
        return FlaisApplicationCrd().apply {
            metadata = ObjectMeta().apply {
                name = "test"

                labels = mutableMapOf(
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
        listOf(FlaisApplicationCrd::class.java, IngressRoute::class.java, PGUser::class.java, KafkaUserAndAcl::class.java, OnePasswordItem::class.java, PodMonitor::class.java)
    )

    fun createKoinTestExtension(additionalModule: Module? = null) = KoinTestExtension.create {
        allowOverride(true)
        modules(baseModule, applicationReconcilerModule())
        additionalModule?.let { modules(it) }
    }
}