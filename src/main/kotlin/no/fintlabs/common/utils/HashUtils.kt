package no.fintlabs.common.utils

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import java.util.Objects

private val mapper =
    ObjectMapper()
        .registerKotlinModule()
        .configure(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS, true)

fun createIntHash(map: Map<*, *>): Int = Objects.hashCode(mapper.writeValueAsString(map))
