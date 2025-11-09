package no.fintlabs.job.api.v1alpha1

import io.fabric8.kubernetes.model.annotation.Group
import io.fabric8.kubernetes.model.annotation.Kind
import io.fabric8.kubernetes.model.annotation.Version
import no.fintlabs.common.api.v1alpha1.FlaisResource

@Group("fintlabs.no")
@Version("v1alpha1")
@Kind("Job")
class FlaisJob : FlaisResource<FlaisJobSpec>()
