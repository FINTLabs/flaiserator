package no.fintlabs.application

import io.fabric8.kubernetes.api.model.*
import io.fabric8.kubernetes.api.model.apps.Deployment
import io.fabric8.kubernetes.api.model.apps.DeploymentSpec
import io.javaoperatorsdk.operator.api.config.informer.Informer
import io.javaoperatorsdk.operator.api.reconciler.Context
import io.javaoperatorsdk.operator.processing.dependent.kubernetes.CRUDKubernetesDependentResource
import io.javaoperatorsdk.operator.processing.dependent.kubernetes.KubernetesDependent
import no.fintlabs.Config
import no.fintlabs.application.api.MANAGED_BY_FLAISERATOR_SELECTOR
import no.fintlabs.application.api.ORG_ID_LABEL
import no.fintlabs.application.api.v1alpha1.FlaisApplicationCrd
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject


@KubernetesDependent(
    informer = Informer(labelSelector = MANAGED_BY_FLAISERATOR_SELECTOR)
)
class DeploymentDR : CRUDKubernetesDependentResource<Deployment, FlaisApplicationCrd>(Deployment::class.java), KoinComponent {
    private val config: Config by inject()
    private val logger = getLogger()

    private val kafkaDR by inject<KafkaDR>()
    private val postgresUserDR by inject<PostgresUserDR>()
    private val onePasswordDR by inject<OnePasswordDR>()

    override fun name() = "deployment"

    override fun desired(primary: FlaisApplicationCrd, context: Context<FlaisApplicationCrd>) = Deployment().apply {
        metadata = createObjectMeta(primary)
        spec = DeploymentSpec().apply {
            replicas = primary.spec.replicas
            selector = LabelSelector(null, mapOf("app" to primary.metadata.name))
            template = PodTemplateSpec().apply {
                metadata = cretePodMetadata(primary)
                spec = createPodSpec(primary, context)
            }
            strategy = primary.spec.strategy
        }
    }

    override fun handleUpdate(actual: Deployment, desired: Deployment, primary: FlaisApplicationCrd, context: Context<FlaisApplicationCrd>): Deployment {
        val kubernetesSerialization = context.client.kubernetesSerialization
        val desiredSelector = kubernetesSerialization.convertValue(desired.spec.selector, Map::class.java)
        val actualSelector = kubernetesSerialization.convertValue(actual.spec.selector, Map::class.java)
        val podSelectorMatch = desiredSelector == actualSelector

        if (podSelectorMatch) return super.handleUpdate(actual, desired, primary, context)

        logger.info("Pod selector does not match, recreating deployment ${actual.metadata.name}")
        handleDelete(primary, actual, context)
        return handleCreate(desired, primary, context)
    }

    private fun cretePodMetadata(primary: FlaisApplicationCrd) = createObjectMeta(primary).apply {
        annotations["kubectl.kubernetes.io/default-container"] = primary.metadata.name
        labels["observability.fintlabs.no/loki"] = primary.spec.observability?.logging?.loki?.toString() ?: "true"
    }

    private fun createPodSpec(primary: FlaisApplicationCrd, context: Context<FlaisApplicationCrd>) = PodSpec().apply {
        volumes = createPodVolumes(primary, context)
        containers = listOf(createAppContainer(primary, context))
        imagePullSecrets = createImagePullSecrets(primary)
    }

    private fun createImagePullSecrets(primary: FlaisApplicationCrd) = mutableSetOf<String>()
        .plus(primary.spec.imagePullSecrets)
        .plus(config.imagePullSecrets)
        .map { LocalObjectReference(it) }

    private fun createAppContainer(primary: FlaisApplicationCrd, context: Context<FlaisApplicationCrd>) = Container().apply {
        name = primary.metadata.name
        image = primary.spec.image
        imagePullPolicy = primary.spec.imagePullPolicy
        resources = primary.spec.resources
        ports = createContainerPorts(primary)
        env = createContainerEnv(primary)
        envFrom = createContainerEnvFrom(primary, context)
        ports = createContainerPorts(primary)
        volumeMounts = createContainerVolumeMounts(primary, context)
    }

    private fun createContainerPorts(primary: FlaisApplicationCrd): List<ContainerPort> {
        val ports = mutableListOf(
            ContainerPort().apply {
                name = "http"
                containerPort = primary.spec.port
                protocol = "TCP"
            }
        )

        val metrics = primary.spec.observability?.metrics ?: primary.spec.prometheus
        if (metrics.enabled && metrics.port.toInt() != primary.spec.port) {
            ports.add(ContainerPort().apply {
                name = "metrics"
                containerPort = metrics.port.toInt()
                protocol = "TCP"
            })
        }

        return ports
    }

    private fun createContainerEnv(primary: FlaisApplicationCrd): List<EnvVar> {
        val envVars = primary.spec.env.toMutableList()

        envVars.add(EnvVar("fint.org-id", primary.metadata.labels[ORG_ID_LABEL], null))
        envVars.add(EnvVar("TZ", "Europe/Oslo", null))

        primary.spec.url.basePath?.takeIf { it.isNotBlank() }?.let { basePath ->
            envVars.add(EnvVar("spring.webflux.base-path", basePath, null))
            envVars.add(EnvVar("spring.mvc.servlet.path", basePath, null))
        }

        return envVars.distinctBy { it.name }
    }

    private fun createContainerEnvFrom(primary: FlaisApplicationCrd, context: Context<FlaisApplicationCrd>): List<EnvFromSource> {
        val envFromSources = listOfNotNull(
            EnvFromSource().apply {
                secretRef = SecretEnvSource().apply {
                    name = "${primary.metadata.name}-op"
                }
            }.takeIf { onePasswordDR.shouldReconcile(primary, context) },
            EnvFromSource().apply {
                secretRef = SecretEnvSource().apply {
                    name = "${primary.metadata.name}-db"
                }
            }.takeIf { postgresUserDR.shouldReconcile(primary, context) },
            EnvFromSource().apply {
                secretRef = SecretEnvSource().apply {
                    name = "${primary.metadata.name}-kafka"
                }
            }.takeIf { kafkaDR.shouldReconcile(primary, context) }
        )

        return primary.spec.envFrom.toMutableSet()
            .plus(envFromSources)
            .toList()
    }

    // Volumes and volume mounts
    private fun createPodVolumes(primary: FlaisApplicationCrd, context: Context<FlaisApplicationCrd>) = listOfNotNull(
        Volume().apply {
            name = "credentials"
            secret = SecretVolumeSource().apply {
                secretName = "${primary.metadata.name}-kafka-certificates"
            }
        }.takeIf { kafkaDR.shouldReconcile(primary, context) }
    )

    private fun createContainerVolumeMounts(primary: FlaisApplicationCrd, context: Context<FlaisApplicationCrd>) = listOfNotNull(
        VolumeMount().apply {
            name = "credentials"
            mountPath = "/credentials"
            readOnly = true
        }.takeIf { kafkaDR.shouldReconcile(primary, context) }
    )
}
