package fr.sacane.jmanager.infrastructure.rest

import fr.sacane.jmanager.domain.models.*
import fr.sacane.jmanager.infrastructure.rest.account.AccountDTO
import fr.sacane.jmanager.infrastructure.rest.session.UserDTO
import fr.sacane.jmanager.infrastructure.rest.sheet.SheetDTO
import org.springframework.http.ResponseEntity

internal fun Account.toDTO(): AccountDTO = AccountDTO(
    this.id ?: throw InvalidRequestException("Impossible d'envoyer null au client"),
    this.sold.toString(),
    this.label,
    this.sheets().map { sheet -> sheet.toDTO() }
)

internal fun SheetDTO.toModel(): Sheet {
    return Sheet(this.id, this.label, this.date, Amount.fromString(this.expenses), Amount.fromString(this.income), Amount.fromString(this.accountAmount), position = this.position)
}
internal fun AccountDTO.toModel(): Account {
    return Account(this.id, Amount.fromString(this.amount), this.labelAccount, this.sheets?.map { it.toModel() }!!.toMutableList())
}
internal fun Sheet.toDTO(): SheetDTO {
    return SheetDTO(this.id!!, this.label, this.expenses.toString(), this.income.toString(), this.date, this.sold.toString(), position = this.position)
}

internal fun User.toDTO(): UserDTO {
    return UserDTO(this.id.id ?: 0, this.username, this.email)
}
internal fun Long.id(): UserId {
    return UserId(this)
}
internal fun <T> Response<T>.toResponseEntity()
: ResponseEntity<T> = when(this.status){
    ResponseState.OK -> mapTo { ResponseEntity.ok(it) }
    ResponseState.NOT_FOUND -> throw NotFoundException(this.message)
    ResponseState.INVALID -> throw InvalidRequestException(this.message)
    ResponseState.FORBIDDEN -> throw ForbiddenException(this.message)
    ResponseState.TIMEOUT  -> throw TimeOutException(this.message)
    ResponseState.UNAUTHORIZED -> throw UnauthorizedRequestException(this.message)
}