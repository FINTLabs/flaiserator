package no.fintlabs.operator

import io.fabric8.kubernetes.api.model.HasMetadata
import io.fabric8.kubernetes.client.KubernetesClient

class KubernetesOperatorContext(
    val namespace: String,
    private val kubernetesClient: KubernetesClient
) {
    inline fun <reified T : HasMetadata> get(name: String): T? {
        return get(T::class.java, name)
    }

    fun <T : HasMetadata> get(clazz: Class<T>, name: String): T? {
        return kubernetesClient.resources(clazz).inNamespace(namespace).withName(name).get()
    }

    fun <T : HasMetadata> create(resource: T): T {
        return kubernetesClient.resource(resource).inNamespace(namespace).create()
    }

    fun <T : HasMetadata> update(resource: T): T {
        return kubernetesClient.resource(resource).inNamespace(namespace).update()
    }

    fun <T : HasMetadata> delete(resource: T) {
        kubernetesClient.resource(resource).inNamespace(namespace).delete()
    }
}