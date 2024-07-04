package no.fintlabs.operator

import io.fabric8.kubernetes.api.model.*
import io.fabric8.kubernetes.api.model.apps.Deployment
import io.fabric8.kubernetes.api.model.apps.DeploymentSpec
import io.javaoperatorsdk.operator.api.reconciler.Context
import io.javaoperatorsdk.operator.processing.dependent.kubernetes.CRUDKubernetesDependentResource
import io.javaoperatorsdk.operator.processing.dependent.kubernetes.KubernetesDependent
import no.fintlabs.Config
import no.fintlabs.operator.api.MANAGED_BY_FLAISERATOR_SELECTOR
import no.fintlabs.operator.api.ORG_ID_LABEL
import no.fintlabs.operator.api.v1alpha1.FlaisApplicationCrd
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

@KubernetesDependent(
    labelSelector = MANAGED_BY_FLAISERATOR_SELECTOR
)
class DeploymentDR : CRUDKubernetesDependentResource<Deployment, FlaisApplicationCrd>(Deployment::class.java), KoinComponent {
    private val config: Config by inject()

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
    }.also { deployment ->
        configureObservability(primary, deployment)
    }

    private fun cretePodMetadata(primary: FlaisApplicationCrd) = createObjectMeta(primary).apply {
        annotations["kubectl.kubernetes.io/default-container"] = primary.metadata.name
    }

    private fun configureObservability(primary: FlaisApplicationCrd, deployment: Deployment) {
        val observability = primary.spec.observability
        val metadata = deployment.spec.template.metadata

        val metrics = observability?.metrics ?: primary.spec.prometheus
        if (metrics.enabled) {
            metadata.annotations["prometheus.io/scrape"] = "true"
            metadata.annotations["prometheus.io/port"] = metrics.port
            metadata.annotations["prometheus.io/path"] = metrics.path
        }

        metadata.labels["observability.fintlabs.no/loki"] = observability?.logging?.loki?.toString() ?: "true"
    }

    private fun createPodSpec(primary: FlaisApplicationCrd, context: Context<FlaisApplicationCrd>) = PodSpec().apply {
        volumes = createPodVolumes(primary, context)
        containers = listOf(createContainer(primary, context))
        imagePullSecrets = createImagePullSecrets(primary)
    }

    private fun createImagePullSecrets(primary: FlaisApplicationCrd) = mutableSetOf<String>()
        .plus(primary.spec.imagePullSecrets)
        .plus(config.imagePullSecrets)
        .map { LocalObjectReference(it) }

    private fun createContainer(primary: FlaisApplicationCrd, context: Context<FlaisApplicationCrd>) = Container().apply {
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

    private fun createContainerPorts(primary: FlaisApplicationCrd) = listOf(
        ContainerPort().apply {
            name = "http"
            containerPort = primary.spec.port
            protocol = "TCP"
        }
    )

    private fun createContainerEnv(primary: FlaisApplicationCrd): List<EnvVar> {
        val envVars = primary.spec.env.toMutableList()

        envVars.add(EnvVar("fint.org-id", primary.metadata.labels[ORG_ID_LABEL], null))
        envVars.add(EnvVar("TZ", "Europe/Oslo", null))

        primary.spec.url.basePath?.let { basePath ->
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
            }.takeIf { createOnePasswordCondition.isMet(null, primary, context) },
            EnvFromSource().apply {
                secretRef = SecretEnvSource().apply {
                    name = "${primary.metadata.name}-db"
                }
            }.takeIf { createPostgresUserCondition.isMet(null, primary, context) },
            EnvFromSource().apply {
                secretRef = SecretEnvSource().apply {
                    name = "${primary.metadata.name}-kafka"
                }
            }.takeIf { creteKafkaCondition.isMet(null, primary, context) }
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
        }.takeIf { creteKafkaCondition.isMet(null, primary, context) }
    )

    private fun createContainerVolumeMounts(primary: FlaisApplicationCrd, context: Context<FlaisApplicationCrd>) = listOfNotNull(
        VolumeMount().apply {
            name = "credentials"
            mountPath = "/credentials"
            readOnly = true
        }.takeIf { creteKafkaCondition.isMet(null, primary, context) }
    )

    companion object {
        const val COMPONENT = "deployment"

        val creteKafkaCondition = no.fintlabs.operator.CreateKafkaCondition()
        val createPostgresUserCondition = CreatePostgresUserCondition()
        val createOnePasswordCondition = CreateOnePasswordCondition()
    }
}