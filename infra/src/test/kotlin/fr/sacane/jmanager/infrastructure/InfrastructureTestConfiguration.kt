package fr.sacane.jmanager.infrastructure

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.databind.JsonSerializer
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializerProvider
import com.fasterxml.jackson.databind.module.SimpleModule
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@TestConfiguration
class InfrastructureTestConfiguration {
    // Aucun bean DataSource ici : on laisse Spring Boot s'initialiser à partir
    // des propriétés de datasource (définies par le bootstrap Testcontainers)
}

class TestLocalDateSerializer : JsonSerializer<LocalDate>() {
    private val formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy")

    override fun serialize(value: LocalDate, gen: JsonGenerator, serializers: SerializerProvider) {
        gen.writeString(value.format(formatter))
    }
}

@TestConfiguration
class Config {
    @Bean
    fun objectMapper(): ObjectMapper {
        val module = SimpleModule()
        module.addSerializer(LocalDate::class.java,
            TestLocalDateSerializer()
        )

        return ObjectMapper()
            .findAndRegisterModules()
            .registerModule(module)
    }
}