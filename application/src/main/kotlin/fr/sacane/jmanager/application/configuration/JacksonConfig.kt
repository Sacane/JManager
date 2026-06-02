package fr.sacane.jmanager.application.configuration

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonDeserializer
import com.fasterxml.jackson.databind.module.SimpleModule
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.math.BigDecimal

/**
 * Custom Jackson deserializer for Double that accepts String values
 * to avoid floating-point precision issues when parsing monetary amounts.
 */
class StringToDoubleDeserializer : JsonDeserializer<Double>() {
    override fun deserialize(p: JsonParser, ctxt: DeserializationContext): Double {
        return when {
            p.currentToken.isNumeric -> p.doubleValue
            else -> {
                val text = p.text
                BigDecimal(text).toDouble()
            }
        }
    }
}

@Configuration
class JacksonConfig {

    @Bean
    fun jacksonCustomizer(): Jackson2ObjectMapperBuilderCustomizer {
        return Jackson2ObjectMapperBuilderCustomizer { builder ->
            val module = SimpleModule()
            module.addDeserializer(Double::class.java, StringToDoubleDeserializer())
            module.addDeserializer(Double::class.javaObjectType, StringToDoubleDeserializer())
            builder.modulesToInstall(module)
        }
    }
}
