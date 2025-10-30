package fr.sacane.jmanager.infrastructure.api

import fr.sacane.jmanager.domain.models.*
import fr.sacane.jmanager.domain.models.transaction.regular.RegularTransaction
import fr.sacane.jmanager.domain.models.transaction.Transaction
import fr.sacane.jmanager.domain.utils.Result
import fr.sacane.jmanager.domain.utils.ResultState
import fr.sacane.jmanager.infrastructure.api.booklet.AccountDTO
import fr.sacane.jmanager.infrastructure.api.session.UserDTO
import fr.sacane.jmanager.infrastructure.api.tag.ColorDTO
import fr.sacane.jmanager.infrastructure.api.tag.TagDTO
import fr.sacane.jmanager.infrastructure.api.transaction.RegularTransactionDTO
import fr.sacane.jmanager.infrastructure.api.transaction.TransactionResult
import fr.sacane.jmanager.infrastructure.api.transaction.toDTO
import org.springframework.http.ResponseEntity
import java.awt.Color

internal fun Booklet.toDTO(): AccountDTO = AccountDTO(
    this.id?.toString() ?: throw InternalServerErrorException(111, "Impossible d'envoyer null au client"),
    this.amount.value,
    this.label,
    this.previewAmount.toStringValue(),
    this.sheets().map { sheet -> sheet.toDTO() },
    this.amount.currency.symbol
)

internal fun TransactionResult.toModel(): Transaction
= Transaction(this.id?.let { java.util.UUID.fromString(it) }, this.label, this.date, Amount(this.value), this.isIncome, tag = if(tagDTO == null) Tag("Aucune", isDefault = true) else Tag(label = tagDTO.label, id = tagDTO.tagId?.let { java.util.UUID.fromString(it) }, isDefault = tagDTO.isDefault, color = Color(tagDTO.colorDTO.red,tagDTO.colorDTO.green,tagDTO.colorDTO.blue)), isPreview = isPreview)

internal fun Transaction.toDTO(): TransactionResult {
    return TransactionResult(id?.toString(), label, amount.value, amount.currency.symbol, isIncome, date, tagDTO = tag?.toDTO(), isPreview)
}


internal fun User.toDTO(): UserDTO
= UserDTO(this.id.value?.toString() ?: "", this.username, this.email, creationDate.toString(), roles = this.roles.map { it.name })

internal fun UserForAdmin.toDTO(): UserDTO
= UserDTO(
    this.user.id.value?.toString() ?: "",
    this.user.username,
    this.user.email,
    this.createdDate.toString()
)

internal fun String.id(): UserId = UserId(java.util.UUID.fromString(this))

internal fun <T> Result<T>.toHttpResponse()
: ResponseEntity<T> = when(this.status){
    ResultState.OK -> mapNullable { ResponseEntity.ok(it) }
    ResultState.NOT_FOUND,
    ResultState.TAG_NOT_FOUND,
    ResultState.USER_NOT_FOUND,
    ResultState.BOOKLET_NOT_FOUND, ResultState.TRANSACTION_NOT_FOUND,
    ResultState.TAG_PLACEHOLDER_UNDEFINED,
    ResultState.BOOKLET_LABEL_NOT_EXIST -> throw NotFoundException(this.status.code, this.message)
    ResultState.INVALID, ResultState.REGISTRATION_ERROR,
    ResultState.BOOKLET_LABEL_EXIST,
    ResultState.TRANSACTION_ENTRY_ERROR,
    ResultState.TAG_SHOULD_NOT_BE_DEFAULT,
    ResultState.BAD_REQUEST, ResultState.INFRASTRUCTURE_ERROR,
    ResultState.CSV_EMPTY_FILE,
    ResultState.CSV_INVALID_HEADER,
    ResultState.CSV_MISSING_COLUMNS,
    ResultState.CSV_EXTRA_COLUMNS,
    ResultState.CSV_MALFORMED_LINE,
    ResultState.CSV_INVALID_DATE_FORMAT,
    ResultState.CSV_INVALID_AMOUNT_FORMAT,
    ResultState.CSV_MISSING_REQUIRED_FIELD,
    ResultState.CSV_BOTH_AMOUNTS_FILLED,
    ResultState.CSV_NO_AMOUNT_FILLED,
    ResultState.CSV_NEGATIVE_AMOUNT,
    ResultState.CSV_POSSIBLE_COLUMN_SWAP -> throw InvalidRequestException(this.status.code, this.message)
    ResultState.TAG_LABEL_ALREADY_TAKEN, ResultState.FORBIDDEN, ResultState.USER_UNAUTHORIZED -> throw ForbiddenException(this.status.code, this.message)
    ResultState.TIMEOUT  -> throw TimeOutException(this.status.code, this.message)
    ResultState.UNAUTHORIZED, ResultState.USER_NOT_AUTHENTICATED,
         ResultState.PASSWORD_NOT_MATCH -> throw UnauthorizedRequestException(this.status.code, this.message)
    ResultState.INTERNAL_SERVER_ERROR -> throw InternalServerErrorException(this.status.code, this.message)
}

internal fun fr.sacane.jmanager.infrastructure.spi.entity.Color.asAwtColor(): Color = Color(this.red, this.green, this.blue)
internal fun ColorDTO.asAwtColor(): Color = Color(this.red, this.green, this.blue)

internal fun Color.toDTO(): ColorDTO = ColorDTO(this.red, this.green, this.blue)

internal fun Tag.toDTO(): TagDTO = TagDTO(tagId = this.id?.toString(), label = this.label, isDefault = this.isDefault, colorDTO = this.color.toDTO())

internal fun TagDTO.toDomain(): Tag = Tag(label = this.label, id = this.tagId?.let { java.util.UUID.fromString(it) }, isDefault = this.isDefault, color = Color(this.colorDTO.red, this.colorDTO.green, this.colorDTO.blue))

internal fun RegularTransaction.toDTO(): RegularTransactionDTO {
    val regularityType = when (this.recurrenceRule) {
        is fr.sacane.jmanager.domain.models.transaction.regular.RecurrenceRule.Monthly -> "MONTHLY"
        is fr.sacane.jmanager.domain.models.transaction.regular.RecurrenceRule.Yearly -> "YEARLY"
        is fr.sacane.jmanager.domain.models.transaction.regular.RecurrenceRule.Weekly -> "WEEKLY"
        is fr.sacane.jmanager.domain.models.transaction.regular.RecurrenceRule.Daily -> "DAILY"
    }

    return RegularTransactionDTO(
        id = this.id.value,
        label = this.label,
        startDate = this.startDate,
        value = this.amount.value,
        isIncome = this.isIncome,
        regularity = regularityType,
        tagDTO = this.tag!!.toDTO(),
        frequencyProperty = frequencyProperty.toDTO()
    )
}