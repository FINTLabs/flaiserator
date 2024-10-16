package no.fintlabs

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import io.fabric8.kubernetes.client.KubernetesClientBuilder
import io.fabric8.kubernetes.client.utils.KubernetesSerialization
import io.javaoperatorsdk.operator.Operator
import io.javaoperatorsdk.operator.api.config.ConfigurationService
import io.javaoperatorsdk.operator.api.reconciler.Reconciler
import no.fintlabs.operator.applicationReconcilerModule
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