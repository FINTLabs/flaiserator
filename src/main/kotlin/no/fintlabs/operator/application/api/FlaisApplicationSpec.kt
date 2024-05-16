package no.fintlabs.operator.application.api

import io.fabric8.generator.annotation.Min
import io.fabric8.generator.annotation.Required
import io.fabric8.generator.annotation.ValidationRule
import io.fabric8.kubernetes.api.model.*
import io.fabric8.kubernetes.api.model.apps.DeploymentStrategy

data class FlaisApplicationSpec(
    @Required
    val orgId: String = "",

    @Min(1.0)
    val replicas: Int = 1,

    @Required
    val image: String = "",

    @ValidationRule(
    "self in ['IfNotPresent', 'Always', 'Never']",
    message = "Invalid imagePullPolicy, must be one of IfNotPresent, Always, Never"
    )
    val imagePullPolicy: String? = null,
    val imagePullSecrets: List<ImagePullSecret> = emptyList(),

    val env: List<EnvVar> = emptyList(),
    val envFrom: List<EnvFromSource> = emptyList(),

    val resources: ResourceRequirements = ResourceRequirementsBuilder()
        .addToRequests("cpu", Quantity("250m"))
        .addToRequests("memory", Quantity("256Mi"))
        .addToLimits("cpu", Quantity("500m"))
        .addToLimits("memory", Quantity("512Mi"))
        .build(),

    @Min(1.0)
    val port: Int = 8080,

    @Deprecated("Does not exist on when applied on DeploymentSpec")
    @ValidationRule(
        "self in ['Always', 'OnFailure', 'Never']",
        message = "Invalid restartPolicy, must be one of Always, OnFailure, Never")
    val restartPolicy: String = "Always",

    val deploymentStrategy: DeploymentStrategy? = null,
    val prometheus: Prometheus = Prometheus(),
    val onePassword: OnePassword? = null,
    val kafka: Kafka = Kafka(),
    val database: Database = Database(),
    val url: Url = Url(),
    val ingress: Ingress = Ingress(),
)