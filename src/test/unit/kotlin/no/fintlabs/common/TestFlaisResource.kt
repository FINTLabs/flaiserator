package no.fintlabs.common

import io.fabric8.kubernetes.api.model.EnvFromSource
import io.fabric8.kubernetes.api.model.EnvVar
import io.fabric8.kubernetes.api.model.ResourceRequirements
import io.fabric8.kubernetes.model.annotation.Group
import io.fabric8.kubernetes.model.annotation.Kind
import io.fabric8.kubernetes.model.annotation.Version
import no.fintlabs.common.api.v1alpha1.FlaisResource
import no.fintlabs.common.api.v1alpha1.FlaisResourceSpec
import no.fintlabs.common.api.v1alpha1.Observability

data class TestSpec(
    override val orgId: String = "org",
    override val image: String = "image",
    override val imagePullPolicy: String? = null,
    override val imagePullSecrets: List<String> = emptyList(),
    override val env: List<EnvVar> = emptyList(),
    override val envFrom: List<EnvFromSource> = emptyList(),
    override val resources: ResourceRequirements = ResourceRequirements(),
    override val observability: Observability? = null,
) : FlaisResourceSpec

@Group("fintlabs.no")
@Version("v1alpha1")
@Kind("Test")
class TestResource : FlaisResource<TestSpec>()
