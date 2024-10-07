package fr.sacane.jmanager.infrastructure.rest

import fr.sacane.jmanager.domain.models.*
import fr.sacane.jmanager.infrastructure.rest.account.AccountDTO
import fr.sacane.jmanager.infrastructure.rest.session.UserDTO
import fr.sacane.jmanager.infrastructure.rest.transaction.SheetDTO
import fr.sacane.jmanager.infrastructure.rest.tag.ColorDTO
import fr.sacane.jmanager.infrastructure.rest.tag.TagDTO
import org.springframework.http.ResponseEntity
import java.awt.Color
import java.math.BigDecimal

internal fun Account.toDTO(): AccountDTO = AccountDTO(
    this.id ?: throw InvalidRequestException("Impossible d'envoyer null au client"),
    this.sold.toStringValue(),
    this.label,
    this.sheets().map { sheet -> sheet.toDTO() }
)

internal fun SheetDTO.toModel(): Transaction
= Transaction(this.id, this.label, this.date, Amount(BigDecimal(this.value)), this.isIncome, tag = if(tagDTO == null) Tag("Aucune", isDefault = true) else Tag(label = tagDTO.label, id = tagDTO.tagId, isDefault = tagDTO.isDefault))

internal fun AccountDTO.toModel(user: User? = null): Account
= Account(this.id, Amount.fromString(this.amount), this.labelAccount, this.sheets?.map { it.toModel() }?.toMutableList() ?: throw IllegalStateException("Impossible to send null sheets"), user)

internal fun Transaction.toDTO(): SheetDTO {
    return SheetDTO(this.id, this.label, this.amount.toStringValue(), this.amount.currency, this.isIncome, this.date, tagDTO = this.tag.toDTO())
}


internal fun User.toDTO(): UserDTO
= UserDTO(this.id.id ?: 0, this.username, this.email)

internal fun Long.id(): UserId = UserId(this)

internal fun <T> Response<T>.toResponseEntity()
: ResponseEntity<T> = when(this.status){
    ResponseState.OK -> mapTo { ResponseEntity.ok(it) }
    ResponseState.NOT_FOUND -> throw NotFoundException(this.message)
    ResponseState.INVALID -> throw InvalidRequestException(this.message)
    ResponseState.FORBIDDEN -> throw ForbiddenException(this.message)
    ResponseState.TIMEOUT  -> throw TimeOutException(this.message)
    ResponseState.UNAUTHORIZED -> throw UnauthorizedRequestException(this.message)
}

internal fun fr.sacane.jmanager.infrastructure.spi.entity.Color.asAwtColor(): Color = Color(this.red, this.green, this.blue)
internal fun ColorDTO.asAwtColor(): Color = Color(this.red, this.green, this.blue)

internal fun Color.toDTO(): ColorDTO = ColorDTO(this.red, this.green, this.blue)

internal fun Tag.toDTO(): TagDTO = TagDTO(tagId = this.id!!, label = this.label, isDefault = this.isDefault, colorDTO = this.color.toDTO()).also { print(it) }