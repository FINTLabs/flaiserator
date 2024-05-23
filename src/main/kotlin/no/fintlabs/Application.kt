package no.fintlabs

import io.fabric8.kubernetes.client.KubernetesClientBuilder
import io.javaoperatorsdk.operator.Operator
import io.javaoperatorsdk.operator.api.config.ConfigurationService
import io.javaoperatorsdk.operator.api.reconciler.Reconciler
import no.fintlabs.operator.application.applicationReconcilerModule
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
    single { KubernetesClientBuilder().build() }
    single {
        Operator(ConfigurationService.newOverriddenConfigurationService { it.withKubernetesClient(get()) }).apply {
            getAll<Reconciler<*>>().forEach { register(it) }
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
