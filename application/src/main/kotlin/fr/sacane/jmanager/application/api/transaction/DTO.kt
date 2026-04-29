package fr.sacane.jmanager.application.api.transaction

import fr.sacane.jmanager.domain.models.transaction.regular.FrequencyProperty
import fr.sacane.jmanager.application.api.tag.TagDTO
import fr.sacane.jmanager.application.configuration.BigDecimalSerializer
import fr.sacane.jmanager.application.configuration.FrequencyPropertyTypeSerializer
import fr.sacane.jmanager.application.configuration.LocalDateSerializer
import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotEmpty
import jakarta.validation.constraints.Size
import kotlinx.serialization.Serializable
import java.math.BigDecimal
import java.time.LocalDate

@Serializable
data class UserBookletIdsTransactionRequest(
    @field:NotBlank
    val bookletId: String,
    @field:Valid
    val transaction: TransactionResult
)


@Serializable
data class TransactionResult(
    val id: String?,
    @field:NotBlank
    @field:Size(max = 100)
    val label: String,
    @Serializable(with = BigDecimalSerializer::class)
    val value: BigDecimal,
    val currency: String = "€",
    val isIncome: Boolean,
    @Serializable(with = LocalDateSerializer::class)
    val date: LocalDate,
    @field:Valid
    val tagDTO: TagDTO? = null,
    val isPreview: Boolean,
    val regularTransactionId: String? = null
)

@Serializable
data class TransactionListResponse(
    val transactions: List<TransactionResult>,
    val amount: String,
    val previewAmount: String,
    val pageNumber: Int = 0,
    val pageSize: Int = 10,
    val totalElements: Long = 0L,
    val totalPages: Int = 1,
)

@Serializable
data class TransactionResponse(
    val id: String,
    val label: String,
    @Serializable(with = LocalDateSerializer::class)
    val date: LocalDate,
    val value: String,
    val isIncome: Boolean,
    val tagDTO: TagDTO,
    val bookletAmount: String,
    val isPreview: Boolean
)

@Serializable
data class UserBookletResponse(
    val bookletLabel: String,
    val transactionResult: TransactionResult
)

@Serializable
data class BookletTransactionsIdRequest(
    @field:NotBlank
    val bookletId: String,
    val transactionIds: List<String> = emptyList(),
    val virtualTransactions: List<VirtualTransactionDescriptorDTO> = emptyList()
)

@Serializable
data class VirtualTransactionDescriptorDTO(
    @field:NotBlank
    val regularTransactionId: String,
    val month: Int,
    val year: Int
)

@Serializable
data class RegularTransactionDTO(
    val id: String,
    val label: String,
    @Serializable(with = LocalDateSerializer::class)
    val startDate: LocalDate,
    @Serializable(with = BigDecimalSerializer::class)
    val value: BigDecimal,
    val isIncome: Boolean,
    val regularity: String,
    val tagDTO: TagDTO,
    val frequencyProperty: FrequencyPropertyDTO,
    val bookletIds: List<String> = emptyList(),
)

@Serializable
data class MonthlyRegularTransactionRequest (
    @field:NotBlank
    @field:Size(max = 100)
    val label: String,
    @Serializable(with = BigDecimalSerializer::class)
    val value: BigDecimal,
    @Serializable(with = LocalDateSerializer::class)
    val startDate: LocalDate,
    val isIncome: Boolean,
    @field:Valid
    val tagDTO: TagDTO,
    @field:Valid
    val frequencyProperty: FrequencyPropertyDTO,
    @field:NotEmpty
    val bookletIds: List<String>,
    val repeatDay: Int?,
)

enum class FrequencyPropertyType {
    FOREVER,
    UNTIL_DATE,
    TIMES
}

@Serializable
data class FrequencyPropertyDTO(
    @Serializable(with = FrequencyPropertyTypeSerializer::class)
    val type: FrequencyPropertyType,
    @Serializable(with = LocalDateSerializer::class)
    val untilDate: LocalDate? = null,
    val times: Int? = null
) {
    init {
        when (type) {
            FrequencyPropertyType.FOREVER -> {
                require(untilDate == null) { "untilDate must be null when type is FOREVER" }
                require(times == null) { "times must be null when type is FOREVER" }
            }
            FrequencyPropertyType.UNTIL_DATE -> {
                require(untilDate != null) { "untilDate must be provided when type is UNTIL_DATE" }
                require(times == null) { "times must be null when type is UNTIL_DATE" }
            }
            FrequencyPropertyType.TIMES -> {
                require(untilDate == null) { "untilDate must be null when type is TIMES" }
                require(times != null && times > 0) { "times must be a positive integer when type is TIMES" }
            }
        }
    }
}

@Serializable
data class UpdateRegularTransactionRequest(
    @field:NotBlank
    val id: String,
    @field:NotBlank
    @field:Size(max = 100)
    val label: String,
    @Serializable(with = BigDecimalSerializer::class)
    val value: BigDecimal,
    @Serializable(with = LocalDateSerializer::class)
    val startDate: LocalDate,
    val isIncome: Boolean,
    @field:Valid
    val tagDTO: TagDTO,
    @field:Valid
    val frequencyProperty: FrequencyPropertyDTO,
    @field:NotEmpty
    val bookletIds: List<String>,
    val recurrenceRule: RecurrenceRuleDTO
)

@Serializable
data class RegularTransactionsDeletionRequest(
    @field:NotEmpty
    val transactionIds: List<String>
)

@Serializable
data class RegularTransactionsDeletionResponse(
    val deletedIds: List<String>
)

@Serializable
data class RecurrenceRuleDTO(
    val type: String,
    val dayOfMonth: Int? = null
)


fun FrequencyPropertyDTO.frequencyToDomain(): FrequencyProperty {
    return when (type) {
        FrequencyPropertyType.FOREVER -> FrequencyProperty.Forever()
        FrequencyPropertyType.UNTIL_DATE -> FrequencyProperty.UntilDate(this.untilDate!!)
        FrequencyPropertyType.TIMES -> FrequencyProperty.SpecificRepetitionTimes(this.times!!)
    }
}

fun FrequencyProperty.toDTO(): FrequencyPropertyDTO {
    return when(this){
        is FrequencyProperty.Forever -> FrequencyPropertyDTO(type = FrequencyPropertyType.FOREVER)
        is FrequencyProperty.UntilDate -> FrequencyPropertyDTO(type = FrequencyPropertyType.UNTIL_DATE, untilDate = this.date)
        is FrequencyProperty.SpecificRepetitionTimes -> FrequencyPropertyDTO(type = FrequencyPropertyType.TIMES, times = this.number)
    }
}

fun RecurrenceRuleDTO.toDomain(defaultMonthlyDay: Int = 1): fr.sacane.jmanager.domain.models.transaction.regular.RecurrenceRule {
    return when(type.uppercase()) {
        "MONTHLY" -> fr.sacane.jmanager.domain.models.transaction.regular.RecurrenceRule.Monthly(dayOfMonth ?: defaultMonthlyDay)
        "YEARLY" -> {
            val month = dayOfMonth?.div(100) ?: 1
            val day = dayOfMonth?.rem(100) ?: 1
            fr.sacane.jmanager.domain.models.transaction.regular.RecurrenceRule.Yearly(month, day)
        }
        else -> throw IllegalArgumentException("Unknown recurrence rule type: $type")
    }
}
