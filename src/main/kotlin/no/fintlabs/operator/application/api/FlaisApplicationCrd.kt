package no.fintlabs.operator.application.api

import io.fabric8.kubernetes.api.model.Namespaced
import io.fabric8.kubernetes.client.CustomResource
import io.fabric8.kubernetes.model.annotation.Group
import io.fabric8.kubernetes.model.annotation.Kind
import io.fabric8.kubernetes.model.annotation.Version

@Group("fintlabs.no")
@Version("v1alpha1")
@Kind("Application")
class FlaisApplicationCrd : CustomResource<FlaisApplicationSpec, FlaisApplicationStatus>(), Namespaced {
    override fun initSpec(): FlaisApplicationSpec {
        return FlaisApplicationSpec()
    }
}