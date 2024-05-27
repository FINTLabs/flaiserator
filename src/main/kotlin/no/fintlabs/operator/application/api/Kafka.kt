package no.fintlabs.operator.application.api

import no.fintlabs.v1alpha1.kafkauserandaclspec.Acls

data class Kafka(@Deprecated("Is going to be removed") val enabled: Boolean = true, val acls: List<Acls> = emptyList())