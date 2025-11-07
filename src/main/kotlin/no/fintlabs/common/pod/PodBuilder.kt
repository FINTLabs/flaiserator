package no.fintlabs.common.pod

import io.fabric8.kubernetes.api.model.EnvVar
import io.fabric8.kubernetes.api.model.LocalObjectReference
import io.fabric8.kubernetes.api.model.ObjectMeta
import io.fabric8.kubernetes.api.model.PodSpec
import io.fabric8.kubernetes.api.model.PodTemplateSpec
import io.javaoperatorsdk.operator.api.reconciler.Context
import no.fintlabs.Config
import no.fintlabs.application.api.ORG_ID_LABEL
import no.fintlabs.common.api.v1alpha1.FlaisResource

class PodBuilder<T : FlaisResource<*>> (
  private val config: Config,
  private val customizers: List<PodCustomizer<T>>
) {
  fun build(
    primary: T,
    context: Context<T>,
    buildMetadata: (builderContext: PodBuilderContext) -> ObjectMeta,
    configurePodSpec: (builderContext: PodBuilderContext) -> Unit
  ): PodTemplateSpec {
    val builderContext = PodBuilderContext()
    createContainerEnvVars(primary, builderContext)

    customizers.forEach { it.customizePod(primary, builderContext, context) }

    val metadata = buildMetadata(builderContext)
    configurePodSpec(builderContext)

    val appName = primary.metadata.name
    val appContainerIndex = builderContext.containers.indexOfFirst { it.name == primary.metadata.name }
    if (appContainerIndex == -1) {
      error("App container '$appName' not found in Pod configuration")
    }

    if (appContainerIndex != 0) {
      builderContext.containers.addFirst(
        builderContext.containers.removeAt(appContainerIndex)
      )
    }

    return PodTemplateSpec().apply {
      this.metadata = metadata.apply {
        annotations.putAll(builderContext.annotations)
        labels.putAll(builderContext.labels)
      }
      spec = PodSpec().apply {
        containers = builderContext.containers
        initContainers = builderContext.initContainers
        volumes = builderContext.volumes
        imagePullSecrets = createImagePullSecrets(primary)
      }
    }
  }

  private fun createContainerEnvVars(primary: T, builderContext: PodBuilderContext) {
    val envVars =
      primary.spec.env
        .map {
          if (it.value?.isEmpty() == true) {
            it.value = null
          }
          it
        }
        .toMutableList()

    envVars.add(EnvVar("fint.org-id", primary.metadata.labels[ORG_ID_LABEL], null))
    envVars.add(EnvVar("TZ", "Europe/Oslo", null))

    builderContext.env.addAll(envVars)
  }

  private fun createImagePullSecrets(primary: T) =
    mutableSetOf<String>().plus(primary.spec.imagePullSecrets).plus(config.imagePullSecrets).map {
      LocalObjectReference(it)
    }

  companion object {
    fun <T : FlaisResource<*>> create(config: Config, vararg customizer: PodCustomizer<T>) = PodBuilder(
      config,
      customizer.toList()
    )
  }
}