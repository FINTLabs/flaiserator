package no.fintlabs.operator.application.api

import no.fintlabs.v1alpha1.KafkaUserAndAclSpec

data class Kafka(var enabled: Boolean = false) : KafkaUserAndAclSpec()