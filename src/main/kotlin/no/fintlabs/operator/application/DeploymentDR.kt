package no.fintlabs.operator.application

import io.fabric8.kubernetes.api.model.*
import io.fabric8.kubernetes.api.model.apps.Deployment
import io.fabric8.kubernetes.api.model.apps.DeploymentSpec
import io.javaoperatorsdk.operator.api.reconciler.Context
import io.javaoperatorsdk.operator.processing.dependent.kubernetes.CRUDKubernetesDependentResource
import io.javaoperatorsdk.operator.processing.dependent.kubernetes.KubernetesDependent
import no.fintlabs.Config
import no.fintlabs.operator.application.api.FlaisApplicationCrd
import no.fintlabs.operator.application.api.ORG_ID_LABEL
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

@KubernetesDependent
class DeploymentDR : CRUDKubernetesDependentResource<Deployment, FlaisApplicationCrd>(Deployment::class.java), KoinComponent {
    val config: Config by inject()

    override fun desired(primary: FlaisApplicationCrd, context: Context<FlaisApplicationCrd>) = Deployment().apply {
        metadata = createObjectMeta(primary)
        spec = DeploymentSpec().apply {
            replicas = primary.spec.replicas
            selector = LabelSelector(null, mapOf("app" to primary.metadata.name))
            template = PodTemplateSpec().apply {
                metadata = cretePodMetadata(primary)
                spec = createPodSpec(primary, context)
            }
        }
    }

    private fun cretePodMetadata(primary: FlaisApplicationCrd) = createObjectMeta(primary).apply {
        annotations.plus("kubectl.kubernetes.io/default-container" to primary.metadata.name)

        if (primary.spec.prometheus.enabled) {
            annotations.plus("prometheus.io/scrape" to "true")
            annotations.plus("prometheus.io/port" to primary.spec.prometheus.port)
            annotations.plus("prometheus.io/path" to primary.spec.prometheus.path)
        }
    }

    private fun createPodSpec(primary: FlaisApplicationCrd, context: Context<FlaisApplicationCrd>) = PodSpec().apply {
        volumes = createPodVolumes(primary, context)
        containers = listOf(createContainer(primary, context))
        imagePullSecrets = createImagePullSecrets(primary)
    }

    private fun createImagePullSecrets(primary: FlaisApplicationCrd) = mutableSetOf<String>()
        .plus(config.imagePullSecrets)
        .plus(primary.spec.imagePullSecrets)
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

    private fun createContainerEnv(primary: FlaisApplicationCrd): MutableList<EnvVar> {
        val envVars = mutableListOf(
            EnvVar("fint.org-id", primary.metadata.labels[ORG_ID_LABEL], null),
            EnvVar("TZ", "Europe/Oslo", null)
        )

        primary.spec.url.basePath?.let { basePath ->
            envVars.add(EnvVar("spring.webflux.base-path", basePath, null))
            envVars.add(EnvVar("spring.mvc.servlet.path", basePath, null))
        }

        envVars.addAll(primary.spec.env)

        return envVars
    }

    private fun createContainerEnvFrom(primary: FlaisApplicationCrd, context: Context<FlaisApplicationCrd>) = listOfNotNull(
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
    ).plus(primary.spec.envFrom)


    // Volumes and volume mounts
    private fun createPodVolumes(primary: FlaisApplicationCrd, context: Context<FlaisApplicationCrd>) = listOfNotNull(
        Volume().apply {
            name = "credentials"
            secret = SecretVolumeSource().apply {
                secretName = "${primary.metadata.name}-karafka-certificates"
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

        val creteKafkaCondition = CreateKafkaCondition()
        val createPostgresUserCondition = CreatePostgresUserCondition()
        val createOnePasswordCondition = CreateOnePasswordCondition()
    }
}