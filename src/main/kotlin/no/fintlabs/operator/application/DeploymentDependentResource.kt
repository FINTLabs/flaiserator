package no.fintlabs.operator.application

import io.fabric8.kubernetes.api.model.*
import io.fabric8.kubernetes.api.model.apps.Deployment
import io.fabric8.kubernetes.api.model.apps.DeploymentSpec
import io.javaoperatorsdk.operator.api.reconciler.Context
import io.javaoperatorsdk.operator.processing.dependent.kubernetes.CRUDKubernetesDependentResource
import io.javaoperatorsdk.operator.processing.dependent.kubernetes.KubernetesDependent
import no.fintlabs.operator.application.api.FlaisApplicationCrd

@KubernetesDependent
class DeploymentDependentResource : CRUDKubernetesDependentResource<Deployment, FlaisApplicationCrd>(Deployment::class.java) {
    override fun desired(primary: FlaisApplicationCrd, context: Context<FlaisApplicationCrd>) = Deployment().apply {
        metadata = createObjectMeta(primary)
        spec = DeploymentSpec().apply {
            replicas = primary.spec.replicas
            selector = LabelSelector(null, mapOf("app" to primary.metadata.name))
            template = PodTemplateSpec().apply {
                metadata = cretePodMetadata(primary)
                spec = createPodSpec(primary)
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

    private fun createPodSpec(primary: FlaisApplicationCrd) = PodSpec().apply {
        volumes = createPodVolumes(primary)
        containers = listOf(createContainer(primary))
        imagePullSecrets = primary.spec.imagePullSecrets
    }

    private fun createContainer(primary: FlaisApplicationCrd) = Container().apply {
        name = primary.metadata.name
        image = primary.spec.image
        imagePullPolicy = primary.spec.imagePullPolicy
        resources = primary.spec.resources
        ports = createContainerPorts(primary)
        env = createContainerEnv(primary)
        envFrom = createContainerEnvFrom(primary)
        ports = createContainerPorts(primary)
        volumeMounts = createContainerVolumeMounts(primary)
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
            EnvVar("fint.org-id", primary.metadata.labels["fintlabs.no/org-id"], null),
            EnvVar("TZ", "Europe/Oslo", null)
        )

        primary.spec.url.basePath?.let { basePath ->
            envVars.add(EnvVar("spring.webflux.base-path", basePath, null))
            envVars.add(EnvVar("spring.mvc.servlet.path", basePath, null))
        }

        envVars.addAll(primary.spec.env)

        return envVars
    }

    private fun createContainerEnvFrom(primary: FlaisApplicationCrd) = listOfNotNull(
        EnvFromSource().apply {
            secretRef = SecretEnvSource().apply {
                name = "${primary.metadata.name}-op"
            }
        }.takeIf { primary.spec.onePassword != null },
        EnvFromSource().apply {
            secretRef = SecretEnvSource().apply {
                name = "${primary.metadata.name}-db"
            }
        }.takeIf { primary.spec.database.enabled },
        EnvFromSource().apply {
            secretRef = SecretEnvSource().apply {
                name = "${primary.metadata.name}-kafka"
            }
        }.takeIf { primary.spec.kafka.enabled }
    ).plus(primary.spec.envFrom)


    // Volumes and volume mounts
    private fun createPodVolumes(primary: FlaisApplicationCrd) = listOfNotNull(
        Volume().apply {
            name = "credentials"
            secret = SecretVolumeSource().apply {
                secretName = "${primary.metadata.name}-karafka-certificates"
            }
        }.takeIf { primary.spec.kafka.enabled }
    )

    private fun createContainerVolumeMounts(primary: FlaisApplicationCrd) = listOfNotNull(
        VolumeMount().apply {
            name = "credentials"
            mountPath = "/credentials"
            readOnly = true
        }.takeIf { primary.spec.kafka.enabled }
    )

    companion object {
        const val COMPONENT = "deployment"
    }
}