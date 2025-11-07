package no.fintlabs.application

import io.fabric8.kubernetes.api.model.Container
import io.fabric8.kubernetes.api.model.ContainerPort
import io.fabric8.kubernetes.api.model.EnvVar
import io.fabric8.kubernetes.api.model.HTTPGetAction
import io.fabric8.kubernetes.api.model.IntOrString
import io.fabric8.kubernetes.api.model.LabelSelector
import io.fabric8.kubernetes.api.model.Probe
import io.fabric8.kubernetes.api.model.apps.Deployment
import io.fabric8.kubernetes.api.model.apps.DeploymentSpec
import io.javaoperatorsdk.operator.api.config.informer.Informer
import io.javaoperatorsdk.operator.api.reconciler.Context
import io.javaoperatorsdk.operator.processing.dependent.kubernetes.CRUDKubernetesDependentResource
import io.javaoperatorsdk.operator.processing.dependent.kubernetes.KubernetesDependent
import no.fintlabs.Config
import no.fintlabs.application.api.MANAGED_BY_FLAISERATOR_SELECTOR
import no.fintlabs.application.api.v1alpha1.FlaisApplication
import no.fintlabs.common.KafkaDR
import no.fintlabs.common.OnePasswordDR
import no.fintlabs.common.PostgresUserDR
import no.fintlabs.common.createObjectMeta
import no.fintlabs.common.getLogger
import no.fintlabs.common.pod.PodBuilder
import no.fintlabs.common.pod.PodBuilderContext
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import no.fintlabs.common.api.v1alpha1.Probe as FlaisProbe

@KubernetesDependent(informer = Informer(labelSelector = MANAGED_BY_FLAISERATOR_SELECTOR))
class DeploymentDR :
    CRUDKubernetesDependentResource<Deployment, FlaisApplication>(Deployment::class.java),
    KoinComponent {
  private val config: Config by inject()
  private val logger = getLogger()

  private val kafkaDR by inject<KafkaDR<FlaisApplication>>()
  private val postgresUserDR by inject<PostgresUserDR<FlaisApplication>>()
  private val onePasswordDR by inject<OnePasswordDR<FlaisApplication>>()
  private val podBuilder = PodBuilder.create(
    config,
    kafkaDR,
    postgresUserDR,
    onePasswordDR
  )

  override fun name() = "deployment"

  override fun desired(primary: FlaisApplication, context: Context<FlaisApplication>): Deployment {
    val podTemplate = podBuilder.build(
      primary, context,
      { builderContext -> cretePodMetadata(primary, builderContext) },
      { builderContext -> configurePodSpec(primary, builderContext) }
    )

    return Deployment().apply {
      metadata = createObjectMeta(primary)
      spec =
        DeploymentSpec().apply {
          replicas = primary.spec.replicas
          selector = LabelSelector(null, mapOf("app" to primary.metadata.name))
          template = podTemplate
          strategy = primary.spec.strategy
        }
    }
  }

  override fun handleUpdate(
      actual: Deployment,
      desired: Deployment,
      primary: FlaisApplication,
      context: Context<FlaisApplication>,
  ): Deployment {
    val kubernetesSerialization = context.client.kubernetesSerialization
    val desiredSelector =
        kubernetesSerialization.convertValue(desired.spec.selector, Map::class.java)
    val actualSelector = kubernetesSerialization.convertValue(actual.spec.selector, Map::class.java)
    val podSelectorMatch = desiredSelector == actualSelector

    if (podSelectorMatch) return super.handleUpdate(actual, desired, primary, context)

    logger.info("Pod selector does not match, recreating deployment ${actual.metadata.name}")
    handleDelete(primary, actual, context)
    return handleCreate(desired, primary, context)
  }

  private fun cretePodMetadata(primary: FlaisApplication, builderContext: PodBuilderContext) =
      createObjectMeta(primary).apply {
        annotations.putAll(builderContext.annotations)
        labels.putAll(builderContext.labels)

        annotations["kubectl.kubernetes.io/default-container"] = primary.metadata.name
        labels["observability.fintlabs.no/loki"] =
            primary.spec.observability?.logging?.loki?.toString() ?: "true"
      }

  private fun configurePodSpec(primary: FlaisApplication, builderContext: PodBuilderContext) {
    createContainerEnv(primary, builderContext)
    builderContext.envFrom.addAll(primary.spec.envFrom)

    builderContext.containers += Container().apply {
      name = primary.metadata.name
      image = primary.spec.image
      imagePullPolicy = primary.spec.imagePullPolicy
      resources = primary.spec.resources
      ports = createContainerPorts(primary)
      env = builderContext.env
      envFrom = builderContext.envFrom
      volumeMounts = builderContext.volumeMounts
      startupProbe = primary.spec.probes?.startup?.let { createPodProbe(it, primary.spec.port) }
      readinessProbe = primary.spec.probes?.readiness?.let { createPodProbe(it, primary.spec.port) }
      livenessProbe = primary.spec.probes?.liveness?.let { createPodProbe(it, primary.spec.port) }
    }
  }

  private fun createContainerPorts(primary: FlaisApplication): List<ContainerPort> {
    val ports =
        mutableListOf(
            ContainerPort().apply {
              name = "http"
              containerPort = primary.spec.port
              protocol = "TCP"
            }
        )

    val metrics = primary.spec.observability?.metrics ?: primary.spec.prometheus
    if (metrics.enabled && metrics.port.toInt() != primary.spec.port) {
      ports.add(
          ContainerPort().apply {
            name = "metrics"
            containerPort = metrics.port.toInt()
            protocol = "TCP"
          }
      )
    }

    return ports
  }

  private fun createContainerEnv(primary: FlaisApplication, builderContext: PodBuilderContext) {
    primary.spec.url.basePath
        ?.takeIf { it.isNotBlank() }
        ?.let { basePath ->
          builderContext.env.add(EnvVar("spring.webflux.base-path", basePath, null))
          builderContext.env.add(EnvVar("spring.mvc.servlet.path", basePath, null))
        }
  }

  private fun createPodProbe(probe: FlaisProbe, appPort: Int) =
      Probe().apply {
        httpGet =
            HTTPGetAction().apply {
              path = probe.path.ensureLeadingSlash()
              port = probe.port ?: IntOrString(appPort)
            }
        initialDelaySeconds = probe.initialDelaySeconds.takeIfPositive()
        failureThreshold = probe.failureThreshold.takeIfPositive()
        periodSeconds = probe.periodSeconds.takeIfPositive()
        timeoutSeconds = probe.timeoutSeconds.takeIfPositive()
      }

  /**
   * Returns this value if > 0; otherwise null.
   *
   * Setting zero causes JOSDK to send zero (which Kubernetes then overrides to its default),
   * causing unnecessary updates. Returning null makes JOSDK omit the field so Kubernetes can apply
   * its default cleanly.
   */
  private fun Int?.takeIfPositive(): Int? = this?.takeIf { it > 0 }

  private fun String?.ensureLeadingSlash(): String =
      when {
        isNullOrBlank() -> "/"
        startsWith("/") -> this
        else -> "/$this"
      }
}
