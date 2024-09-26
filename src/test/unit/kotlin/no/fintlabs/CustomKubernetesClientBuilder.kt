package no.fintlabs

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import io.fabric8.kubernetes.client.KubernetesClientBuilder
import io.fabric8.kubernetes.client.server.mock.KubernetesClientBuilderCustomizer
import io.fabric8.kubernetes.client.utils.KubernetesSerialization

class CustomKubernetesClientBuilder : KubernetesClientBuilderCustomizer() {
    override fun accept(builder: KubernetesClientBuilder) {
        val mapper = ObjectMapper().registerKotlinModule()
        builder.withKubernetesSerialization(KubernetesSerialization(mapper, true))
    }
}