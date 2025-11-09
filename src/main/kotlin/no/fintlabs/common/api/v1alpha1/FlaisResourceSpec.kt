package no.fintlabs.common.api.v1alpha1

import io.fabric8.generator.annotation.Required
import io.fabric8.generator.annotation.ValidationRule
import io.fabric8.kubernetes.api.model.EnvFromSource
import io.fabric8.kubernetes.api.model.EnvVar
import io.fabric8.kubernetes.api.model.ResourceRequirements

interface FlaisResourceSpec {
  @get:Required val orgId: String
  @get:Required val image: String
  @get:ValidationRule(
      "self in ['IfNotPresent', 'Always', 'Never']",
      message = "Invalid imagePullPolicy, must be one of IfNotPresent, Always, Never",
  )
  val imagePullPolicy: String?
  val imagePullSecrets: List<String>
  val env: List<EnvVar>
  val envFrom: List<EnvFromSource>
  val resources: ResourceRequirements
  val observability: Observability?
}
