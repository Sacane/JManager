package fr.sacane.jmanager.infrastructure.api.transaction

import fr.sacane.jmanager.domain.models.transaction.regular.FrequencyProperty
import fr.sacane.jmanager.infrastructure.api.tag.TagDTO
import fr.sacane.jmanager.infrastructure.configuration.BigDecimalSerializer
import fr.sacane.jmanager.infrastructure.configuration.FrequencyPropertyTypeSerializer
import fr.sacane.jmanager.infrastructure.configuration.LocalDateSerializer
import kotlinx.serialization.Serializable
import java.math.BigDecimal
import java.time.LocalDate
import kotlin.times

@Serializable
data class UserAccountIdsTransactionRequest(
    val accountId: Long,
    val transaction: TransactionResult
)


@Serializable
data class TransactionResult(
    val id: Long?,
    val label: String,
    @Serializable(with = BigDecimalSerializer::class)
    val value: BigDecimal,
    val currency: String = "€",
    val isIncome: Boolean,
    val date: String,
    val tagDTO: TagDTO? = null,
    val isPreview: Boolean
)
data class TransactionListResponse(
    val transactions: List<TransactionResult>
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
    val accountAmount: String,
    val accountPreviewAmount: String,
    val isPreview: Boolean
)

@Serializable
data class UserBookletResponse(
    val accountLabel: String,
    val transactionResult: TransactionResult
)

@Serializable
data class AccountTransactionsIdRequest(
    val accountId: Long,
    val transactionIds: List<Long>
)

@Serializable
data class RegularTransactionCreationRequest(
    val label: String,
    val startDate: String,
    @Serializable(with = BigDecimalSerializer::class)
    val value: BigDecimal,
    val isIncome: Boolean,
    val regularity: String,
    val tagDTO: TagDTO? = null
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
    val tagDTO: TagDTO
)

@Serializable
data class MonthlyRegularTransactionRequest (
    val label: String,
    @Serializable(with = BigDecimalSerializer::class)
    val value: BigDecimal,
    val isIncome: Boolean,
    val tagDTO: TagDTO,
    val frequencyProperty: FrequencyPropertyDTO,
    val frequencyPropertyType: FrequencyPropertyType
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

fun FrequencyPropertyDTO.frequencyToDomain(frequencyProperty: FrequencyPropertyType): FrequencyProperty {
    return when (frequencyProperty) {
        FrequencyPropertyType.FOREVER -> FrequencyProperty.Forever()
        FrequencyPropertyType.UNTIL_DATE -> FrequencyProperty.UntilDate(this.untilDate!!)
        FrequencyPropertyType.TIMES -> FrequencyProperty.SpecificRepetitionTimes(this.times!!)
    }
}