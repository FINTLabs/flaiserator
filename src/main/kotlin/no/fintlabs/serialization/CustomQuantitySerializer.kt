package no.fintlabs.serialization

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.databind.JsonSerializer
import com.fasterxml.jackson.databind.SerializerProvider
import io.fabric8.kubernetes.api.model.Quantity
import java.io.IOException

class CustomQuantitySerializer : JsonSerializer<Quantity>() {
    @Throws(IOException::class)
    override fun serialize(value: Quantity?, gen: JsonGenerator, serializers: SerializerProvider) {
        if (value != null) {
            gen.writeString(value.numericalAmount.stripTrailingZeros().toString());
        } else {
            gen.writeNull();
        }
    }
}