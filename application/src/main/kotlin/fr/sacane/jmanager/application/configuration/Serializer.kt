package fr.sacane.jmanager.application.configuration

import fr.sacane.jmanager.application.api.transaction.FrequencyPropertyType
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import java.time.format.DateTimeFormatter


object BigDecimalSerializer : KSerializer<BigDecimal> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("BigDecimal", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: BigDecimal) {
        encoder.encodeString(value.setScale(2, RoundingMode.HALF_UP).toString())
    }

    override fun deserialize(decoder: Decoder): BigDecimal {
        return BigDecimal(decoder.decodeString()).setScale(2, RoundingMode.HALF_UP)
    }
}

object LocalDateSerializer : KSerializer<LocalDate> {
    private val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("LocalDate", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: LocalDate) {
        encoder.encodeString(value.format(formatter))
    }

    override fun deserialize(decoder: Decoder): LocalDate {
        return LocalDate.parse(decoder.decodeString(), formatter)
    }
}

object FrequencyPropertyTypeSerializer : KSerializer<FrequencyPropertyType> {
    override val descriptor: SerialDescriptor
        get() = PrimitiveSerialDescriptor("Frequency property type", PrimitiveKind.STRING)

    override fun serialize(
        encoder: Encoder,
        value: FrequencyPropertyType
    ) {
        encoder.encodeString(value.name)
    }

    override fun deserialize(decoder: Decoder): FrequencyPropertyType {
        return FrequencyPropertyType.valueOf(decoder.decodeString())
    }

}
