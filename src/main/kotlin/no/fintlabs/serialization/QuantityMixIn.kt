package no.fintlabs.serialization

import com.fasterxml.jackson.databind.annotation.JsonSerialize

@JsonSerialize(using = CustomQuantitySerializer::class)
interface QuantityMixIn