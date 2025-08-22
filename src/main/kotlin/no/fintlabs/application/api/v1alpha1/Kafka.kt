package no.fintlabs.application.api.v1alpha1

import no.fintlabs.v1alpha1.kafkauserandaclspec.Acls

data class Kafka(
    @Deprecated("Is going to be removed") val enabled: Boolean = true,
    val acls: List<Acls> = emptyList()
)
