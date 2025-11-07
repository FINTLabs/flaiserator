package no.fintlabs.application.api.v1alpha1

import io.fabric8.generator.annotation.Min
import io.fabric8.kubernetes.api.model.EnvFromSource
import io.fabric8.kubernetes.api.model.EnvVar
import io.fabric8.kubernetes.api.model.Quantity
import io.fabric8.kubernetes.api.model.ResourceRequirements
import io.fabric8.kubernetes.api.model.ResourceRequirementsBuilder
import io.fabric8.kubernetes.api.model.apps.DeploymentStrategy
import no.fintlabs.common.WithKafka
import no.fintlabs.common.WithOnePassword
import no.fintlabs.common.WithPostgres
import no.fintlabs.common.api.v1alpha1.Database
import no.fintlabs.common.api.v1alpha1.FlaisResourceSpec
import no.fintlabs.common.api.v1alpha1.Kafka
import no.fintlabs.common.api.v1alpha1.OnePassword
import no.fintlabs.common.api.v1alpha1.Probes

data class FlaisApplicationSpec(
    override val orgId: String = "",
    @get:Min(0.0)
    val replicas: Int = 1,
    override val image: String = "",
    override val imagePullPolicy: String? = null,
    override val imagePullSecrets: List<String> = emptyList(),
    override val env: List<EnvVar> = emptyList(),
    override val envFrom: List<EnvFromSource> = emptyList(),
    override val resources: ResourceRequirements =
        ResourceRequirementsBuilder()
            .addToRequests("cpu", Quantity("250m"))
            .addToRequests("memory", Quantity("256Mi"))
            .addToLimits("cpu", Quantity("500m"))
            .addToLimits("memory", Quantity("512Mi"))
            .build(),
    override val probes: Probes? = null,
    override val port: Int = 8080,
    override val restartPolicy: String = "Always",
    override val strategy: DeploymentStrategy? = null,
    override val onePassword: OnePassword? = null,
    override val kafka: Kafka = Kafka(),
    override val database: Database = Database(),
    val prometheus: Metrics = Metrics(),
    val url: Url = Url(),
    val ingress: Ingress? = null,
    override val observability: ApplicationObservability? = null,
) : FlaisResourceSpec, WithOnePassword, WithKafka, WithPostgres
