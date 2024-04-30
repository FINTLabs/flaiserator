package no.fintlabs.operator.application.api

import io.fabric8.generator.annotation.Min
import io.fabric8.generator.annotation.Required
import io.fabric8.generator.annotation.ValidationRule
import io.fabric8.kubernetes.api.model.EnvFromSource
import io.fabric8.kubernetes.api.model.EnvVar
import io.fabric8.kubernetes.api.model.Quantity
import io.fabric8.kubernetes.api.model.ResourceRequirementsBuilder
import io.fabric8.kubernetes.api.model.apps.DeploymentStrategy

class FlaisApplicationSpec {
    @Required
    lateinit var orgId: String

    @Min(1.0)
    var replicas = 1

    @Required
    lateinit var image: String
    @ValidationRule(
        "self in ['IfNotPresent', 'Always', 'Never']",
        message = "Invalid imagePullPolicy, must be one of IfNotPresent, Always, Never"
    )
    var imagePullPolicy: String? = null
    val imagePullSecrets = ArrayList<ImagePullSecret>()

    val env = ArrayList<EnvVar>()
    val envFrom = ArrayList<EnvFromSource>()

    val resources = ResourceRequirementsBuilder()
        .addToRequests("cpu", Quantity("250m"))
        .addToRequests("memory", Quantity("256Mi"))
        .addToLimits("cpu", Quantity("500m"))
        .addToLimits("memory", Quantity("512Mi"))
        .build()

    var port = 8080;
    @ValidationRule(
        "self in ['Always', 'OnFailure', 'Never']",
        message = "Invalid restartPolicy, must be one of Always, OnFailure, Never")
    var restartPolicy = "Always"

    var deploymentStrategy: DeploymentStrategy? = null
    val prometheus = Prometheus()
    var onePassword: OnePassword? = null
    val kafka = Kafka()
    val database = Database()
    val url = Url()
    val ingress = Ingress()
}