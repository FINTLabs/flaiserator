package no.fintlabs.operator.application.api

import no.fintlabs.v1alpha1.KafkaUserAndAclSpec
import no.fintlabs.v1alpha1.kafkauserandaclspec.Acls

data class Kafka(val enabled: Boolean = false, private val acls: List<Acls> = emptyList()) : KafkaUserAndAclSpec() {
    init {
        super.setAcls(acls)
    }
}