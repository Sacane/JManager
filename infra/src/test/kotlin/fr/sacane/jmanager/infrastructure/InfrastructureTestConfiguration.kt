package fr.sacane.jmanager.infrastructure

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonDeserializer
import com.fasterxml.jackson.databind.JsonSerializer
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializerProvider
import com.fasterxml.jackson.databind.module.SimpleModule
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import java.math.BigDecimal
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@TestConfiguration
class InfrastructureTestConfiguration {
}

class TestLocalDateSerializer : JsonSerializer<LocalDate>() {
    private val formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy")

    override fun serialize(value: LocalDate, gen: JsonGenerator, serializers: SerializerProvider) {
        gen.writeString(value.format(formatter))
    }
}

/**
 * Custom Jackson serializer for Double that outputs String values with 2 decimal precision
 * to match frontend behavior and avoid floating-point precision issues.
 */
class TestStringToDoubleSerializer : JsonSerializer<Double>() {
    override fun serialize(value: Double, gen: JsonGenerator, serializers: SerializerProvider) {
        gen.writeString(String.format(java.util.Locale.US, "%.2f", value))
    }
}

/**
 * Custom Jackson deserializer for Double that accepts String values
 * to avoid floating-point precision issues when parsing monetary amounts.
 */
class TestStringToDoubleDeserializer : JsonDeserializer<Double>() {
    override fun deserialize(p: JsonParser, ctxt: DeserializationContext): Double {
        return when {
            p.currentToken.isNumeric -> p.doubleValue
            else -> {
                // Parse string as BigDecimal first for precision, then convert to Double
                val text = p.text
                BigDecimal(text).toDouble()
            }
        }
    }
}

@TestConfiguration
class Config {
    @Bean
    fun objectMapper(): ObjectMapper {
        val module = SimpleModule()
        module.addSerializer(LocalDate::class.java, TestLocalDateSerializer())
        module.addSerializer(Double::class.java, TestStringToDoubleSerializer())
        module.addSerializer(Double::class.javaObjectType, TestStringToDoubleSerializer())
        module.addDeserializer(Double::class.java, TestStringToDoubleDeserializer())
        module.addDeserializer(Double::class.javaObjectType, TestStringToDoubleDeserializer())

        return ObjectMapper()
            .findAndRegisterModules()
            .registerModule(module)
    }
}