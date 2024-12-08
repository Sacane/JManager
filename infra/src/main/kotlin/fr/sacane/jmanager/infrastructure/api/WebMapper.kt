package fr.sacane.jmanager.infrastructure.api

import fr.sacane.jmanager.domain.utils.Response
import fr.sacane.jmanager.domain.utils.ResponseState
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

internal fun <T> Response<T>.toResponseEntity()
: ResponseEntity<T> = when(this.status){
    ResponseState.OK -> mapTo { ResponseEntity.ok(it) }
    ResponseState.NOT_FOUND,
    ResponseState.TAG_NOT_FOUND,
    ResponseState.USER_NOT_FOUND,
    ResponseState.BOOKLET_NOT_FOUND, ResponseState.TRANSACTION_NOT_FOUND,
    ResponseState.TAG_PLACEHOLDER_UNDEFINED,
    ResponseState.BOOKLET_LABEL_NOT_EXIST -> throw NotFoundException(this.message)
    ResponseState.INVALID, ResponseState.REGISTRATION_ERROR,
    ResponseState.BOOKLET_LABEL_EXIST,
    ResponseState.TAG_LABEL_ALREADY_TAKEN, ResponseState.TRANSACTION_ENTRY_ERROR,
    ResponseState.BAD_REQUEST -> throw InvalidRequestException(this.message)
    ResponseState.FORBIDDEN, ResponseState.USER_UNAUTHORIZED -> throw ForbiddenException(this.message)
    ResponseState.TIMEOUT  -> throw TimeOutException(this.message)
    ResponseState.UNAUTHORIZED, ResponseState.USER_NOT_AUTHENTICATED -> throw UnauthorizedRequestException(this.message)
    ResponseState.INTERNAL_SERVER_ERROR -> throw InternalServerErrorException(this.message)
}

internal fun fr.sacane.jmanager.infrastructure.spi.entity.Color.asAwtColor(): Color = Color(this.red, this.green, this.blue)
internal fun ColorDTO.asAwtColor(): Color = Color(this.red, this.green, this.blue)

internal fun Color.toDTO(): ColorDTO = ColorDTO(this.red, this.green, this.blue)

internal fun Tag.toDTO(): TagDTO = TagDTO(tagId = this.id!!, label = this.label, isDefault = this.isDefault, colorDTO = this.color.toDTO())