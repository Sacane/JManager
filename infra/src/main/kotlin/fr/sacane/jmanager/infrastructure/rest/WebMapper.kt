package fr.sacane.jmanager.infrastructure.rest

import fr.sacane.jmanager.domain.models.*
import fr.sacane.jmanager.infrastructure.rest.account.AccountDTO
import fr.sacane.jmanager.infrastructure.rest.session.UserDTO
import fr.sacane.jmanager.infrastructure.rest.sheet.SheetDTO
import fr.sacane.jmanager.infrastructure.rest.tag.ColorDTO
import fr.sacane.jmanager.infrastructure.rest.tag.TagDTO
import org.springframework.http.ResponseEntity
import java.awt.Color

internal fun Account.toDTO(): AccountDTO = AccountDTO(
    this.id ?: throw InvalidRequestException("Impossible d'envoyer null au client"),
    this.sold.toString(),
    this.label,
    this.sheets().map { sheet -> sheet.toDTO() }
)

internal fun SheetDTO.toModel(): Transaction
= Transaction(this.id, this.label, this.date, Amount.fromString(this.expenses), Amount.fromString(this.income), Amount.fromString(this.accountAmount), position = this.position, tag = if(tagDTO == null) Tag("Aucune", isDefault = true) else Tag(label = tagDTO.label, id = tagDTO.tagId, isDefault = tagDTO.isDefault))

internal fun AccountDTO.toModel(user: User? = null): Account
= Account(this.id, Amount.fromString(this.amount), this.labelAccount, this.sheets?.map { it.toModel() }?.toMutableList() ?: throw IllegalStateException("Impossible to send null sheets"), user)

internal fun Transaction.toDTO(): SheetDTO
= SheetDTO(this.id, this.label, this.expenses.toString(), this.income.toString(), this.date, this.sold.toString(), position = this.position)


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

internal fun <T> String.toAmountAsResponse()
: Response<Amount> = try {
    Response.ok(Amount.fromString(this))
}catch (ex: InvalidMoneyFormatException){
    Response.invalid(ex.message ?: "La monnaie est invalide: $this")
}catch (ex: Exception) {
    Response.invalid(ex.message ?: "Une erreur est survenue")
}

internal fun fr.sacane.jmanager.infrastructure.spi.entity.Color.asAwtColor(): Color = Color(this.red, this.green, this.blue)
internal fun ColorDTO.asAwtColor(): Color = Color(this.red, this.green, this.blue)

internal fun Color.toDTO(): ColorDTO = ColorDTO(this.red, this.green, this.blue)

internal fun Tag.toDTO(): TagDTO = TagDTO(tagId = this.id!!, label = this.label, isDefault = this.isDefault)