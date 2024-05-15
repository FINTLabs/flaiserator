package no.fintlabs.operator.application.api

import com.onepassword.v1.OnePasswordItemSpec

data class OnePassword(private val itemPath: String? = null) : OnePasswordItemSpec() {
    init {
        super.setItemPath(itemPath)
    }
}