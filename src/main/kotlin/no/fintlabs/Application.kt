package no.fintlabs

import com.fasterxml.jackson.databind.ObjectMapper
import io.fabric8.kubernetes.api.model.Quantity
import io.fabric8.kubernetes.client.KubernetesClientBuilder
import io.fabric8.kubernetes.client.utils.KubernetesSerialization
import io.javaoperatorsdk.operator.Operator
import io.javaoperatorsdk.operator.api.config.ConfigurationService
import io.javaoperatorsdk.operator.api.reconciler.Reconciler
import no.fintlabs.operator.applicationReconcilerModule
import no.fintlabs.serialization.QuantityMixIn
import org.koin.core.context.startKoin
import org.koin.dsl.module
import org.koin.mp.KoinPlatform.getKoin


fun main() {
    startKoin {
        modules(
            applicationReconcilerModule(),
            baseModule
        )
    }
    startOperator()
}

val baseModule = module {
    single(createdAtStart = true) { defaultConfig() }
    single {
        ObjectMapper().apply {
            addMixIn(Quantity::class.java, QuantityMixIn::class.java)
            registerKotlinModule()
        }
    }
    single {
        KubernetesClientBuilder()
            .withKubernetesSerialization(KubernetesSerialization(get(), true))
            .build()
    }
    single<(Operator) -> Unit> {
        { operator ->
            getAll<Reconciler<*>>().forEach { operator.register(it) }
        }
    }
    single {
        Operator(ConfigurationService.newOverriddenConfigurationService { it.withKubernetesClient(get()) }).apply {
            get<(Operator) -> Unit>().invoke(this)
        }
    }
}

fun startOperator() {
    val operator = getKoin().get<Operator>();

    Runtime.getRuntime().addShutdownHook(Thread {
        operator.stop()
    })

    operator.start()
}