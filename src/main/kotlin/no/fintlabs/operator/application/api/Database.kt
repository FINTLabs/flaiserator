package no.fintlabs.operator.application.api

import no.fintlabs.v1alpha1.PGUserSpec

data class Database(var enabled: Boolean = false) : PGUserSpec()
