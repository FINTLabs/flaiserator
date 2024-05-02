package no.fintlabs.operator.application

import io.javaoperatorsdk.operator.api.reconciler.*
import no.fintlabs.operator.application.api.FlaisApplicationCrd

@ControllerConfiguration
class FlaisApplicationReconciler : Reconciler<FlaisApplicationCrd>, Cleaner<FlaisApplicationCrd> {
    override fun reconcile(resource: FlaisApplicationCrd, context: Context<FlaisApplicationCrd>): UpdateControl<FlaisApplicationCrd> {
        return UpdateControl.noUpdate()
    }

    override fun cleanup(resource: FlaisApplicationCrd, context: Context<FlaisApplicationCrd>): DeleteControl {
        return DeleteControl.defaultDelete()
    }
}