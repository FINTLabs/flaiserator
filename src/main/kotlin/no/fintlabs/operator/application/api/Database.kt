package no.fintlabs.operator.application.api

import no.fintlabs.v1alpha1.PGUserSpec

data class Database(val enabled: Boolean = false, private val database: String = "") : PGUserSpec() {
    init {
        super.setDatabase(database)
    }
}
