package no.fintlabs.extensions

import io.fabric8.kubernetes.api.model.HasMetadata
import io.fabric8.kubernetes.client.KubernetesClient
import io.javaoperatorsdk.operator.Operator
import io.javaoperatorsdk.operator.api.config.ControllerConfigurationOverrider
import io.javaoperatorsdk.operator.api.reconciler.Reconciler

class KubernetesOperatorContext(
    val namespace: String,
    val getKubernetesClient: () -> KubernetesClient,
    val getOperator: () -> Operator,
) {
  val kubernetesClient
    get() = getKubernetesClient()

  val operator
    get() = getOperator()

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

  fun <T : Reconciler<*>> registerReconciler(
      reconciler: T,
      configuration: ((ControllerConfigurationOverrider<*>) -> Unit)? = null,
  ) {
    operator.register(reconciler) {
      it.settingNamespace(kubernetesClient.namespace)
      configuration?.invoke(it)
    }
  }
}
