package fr.sacane.jmanager.infrastructure.api

import fr.sacane.jmanager.domain.utils.Result
import fr.sacane.jmanager.domain.utils.ResultState
import fr.sacane.jmanager.domain.models.*
import fr.sacane.jmanager.infrastructure.api.account.AccountDTO
import fr.sacane.jmanager.infrastructure.api.session.UserDTO
import fr.sacane.jmanager.infrastructure.api.transaction.SheetDTO
import fr.sacane.jmanager.infrastructure.api.tag.ColorDTO
import fr.sacane.jmanager.infrastructure.api.tag.TagDTO
import org.springframework.http.ResponseEntity
import java.awt.Color
import java.math.BigDecimal

internal fun Account.toDTO(): AccountDTO = AccountDTO(
    this.id ?: throw InvalidRequestException("Impossible d'envoyer null au client"),
    this.amount.toStringValue(),
    this.label,
    this.previewAmount.toStringValue(),
    this.sheets().map { sheet -> sheet.toDTO() }
)

internal fun SheetDTO.toModel(): Transaction
= Transaction(this.id, this.label, this.date, Amount(BigDecimal(this.value)), this.isIncome, tag = if(tagDTO == null) Tag("Aucune", isDefault = true) else Tag(label = tagDTO.label, id = tagDTO.tagId, isDefault = tagDTO.isDefault, color = Color(tagDTO.colorDTO.red,tagDTO.colorDTO.green,tagDTO.colorDTO.blue)), isPreview = isPreview)

internal fun AccountDTO.toModel(user: User? = null): Account
= Account(this.id, Amount.fromString(this.amount), this.labelAccount, this.sheets?.map { it.toModel() }?.toMutableList() ?: throw IllegalStateException("Impossible to send null sheets"), user, previewAmount = Amount.fromString(this.amount))

internal fun Transaction.toDTO(): SheetDTO {
    return SheetDTO(id, label, amount.toStringValue(), amount.currency, isIncome, date, tagDTO = tag.toDTO(), isPreview)
}


internal fun User.toDTO(): UserDTO
= UserDTO(this.id.id ?: 0, this.username, this.email)

internal fun Long.id(): UserId = UserId(this)

internal fun <T> Result<T>.toResponseEntity()
: ResponseEntity<T> = when(this.status){
    ResultState.OK -> mapTo { ResponseEntity.ok(it) }
    ResultState.NOT_FOUND,
    ResultState.TAG_NOT_FOUND,
    ResultState.USER_NOT_FOUND,
    ResultState.BOOKLET_NOT_FOUND, ResultState.TRANSACTION_NOT_FOUND,
    ResultState.TAG_PLACEHOLDER_UNDEFINED,
    ResultState.BOOKLET_LABEL_NOT_EXIST -> throw NotFoundException(this.message)
    ResultState.INVALID, ResultState.REGISTRATION_ERROR,
    ResultState.BOOKLET_LABEL_EXIST,
    ResultState.TAG_LABEL_ALREADY_TAKEN, ResultState.TRANSACTION_ENTRY_ERROR,
    ResultState.BAD_REQUEST, ResultState.INFRASTRUCTURE_ERROR -> throw InvalidRequestException(this.message)
    ResultState.FORBIDDEN, ResultState.USER_UNAUTHORIZED -> throw ForbiddenException(this.message)
    ResultState.TIMEOUT  -> throw TimeOutException(this.message)
    ResultState.UNAUTHORIZED, ResultState.USER_NOT_AUTHENTICATED,
         ResultState.PASSWORD_NOT_MATCH -> throw UnauthorizedRequestException(this.message)
    ResultState.INTERNAL_SERVER_ERROR -> throw InternalServerErrorException(this.message)
}

internal fun fr.sacane.jmanager.infrastructure.spi.entity.Color.asAwtColor(): Color = Color(this.red, this.green, this.blue)
internal fun ColorDTO.asAwtColor(): Color = Color(this.red, this.green, this.blue)

internal fun Color.toDTO(): ColorDTO = ColorDTO(this.red, this.green, this.blue)

internal fun Tag.toDTO(): TagDTO = TagDTO(tagId = this.id!!, label = this.label, isDefault = this.isDefault, colorDTO = this.color.toDTO())