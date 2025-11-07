package no.fintlabs.common.api.v1alpha1

import io.fabric8.generator.annotation.Min
import io.fabric8.generator.annotation.Required
import io.fabric8.generator.annotation.ValidationRule
import io.fabric8.kubernetes.api.model.EnvFromSource
import io.fabric8.kubernetes.api.model.EnvVar
import io.fabric8.kubernetes.api.model.ResourceRequirements
import io.fabric8.kubernetes.api.model.apps.DeploymentStrategy

interface FlaisResourceSpec {
  @get:Required
  val orgId: String
  @get:Required
  val image: String
  @get:ValidationRule(
    "self in ['IfNotPresent', 'Always', 'Never']",
    message = "Invalid imagePullPolicy, must be one of IfNotPresent, Always, Never",
  )
  val imagePullPolicy: String?
  val imagePullSecrets: List<String>
  val env: List<EnvVar>
  val envFrom: List<EnvFromSource>
  val resources: ResourceRequirements
  val probes: Probes?
  @get:Min(1.0) val port: Int
  @get:Deprecated("Does not exist on when applied on DeploymentSpec")
  @get:ValidationRule(
    "self in ['Always', 'OnFailure', 'Never']",
    message = "Invalid restartPolicy, must be one of Always, OnFailure, Never",
  )
  val restartPolicy: String
  val strategy: DeploymentStrategy?
  val observability: Observability?
}
