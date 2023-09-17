package fr.sacane.jmanager.infrastructure.rest

import fr.sacane.jmanager.domain.models.*
import fr.sacane.jmanager.infrastructure.rest.account.AccountDTO
import fr.sacane.jmanager.infrastructure.rest.sheet.SheetDTO
import fr.sacane.jmanager.infrastructure.rest.user.RegisteredUserDTO
import fr.sacane.jmanager.infrastructure.rest.user.UserDTO
import org.springframework.http.ResponseEntity

internal fun Account.toDTO(): AccountDTO {
    return AccountDTO(
        this.id!!,
        this.sold,
        this.label,
        this.sheets().map { sheet -> sheet.toDTO() }
    )
}

internal fun SheetDTO.toModel(): Sheet {
    return Sheet(this.id, this.label, this.date, this.expenses, this.income, this.accountAmount, position = this.position)
}
internal fun AccountDTO.toModel(): Account {
    return Account(this.id, this.amount, this.labelAccount, this.sheets?.map { it.toModel() }!!.toMutableList())
}
internal fun RegisteredUserDTO.toModel(): User {
    return User(UserId(0), this.username, this.email, mutableListOf(), Password(this.password), mutableListOf(
        CategoryFactory.DEFAULT_CATEGORY))
}

internal fun Sheet.toDTO(): SheetDTO {
    return SheetDTO(this.id!!, this.label, this.expenses, this.income, this.date, this.sold, position = this.position)
}

internal fun User.toDTO(): UserDTO {
    return UserDTO(this.id.id ?: 0, this.username, this.email)
}
internal fun Long.id(): UserId {
    return UserId(this)
}
internal fun <T> Response<T>.toResponseEntity(): ResponseEntity<T>{
    return when(this.status){
        ResponseState.OK -> mapTo { ResponseEntity.ok(it) }
        ResponseState.TIMEOUT, ResponseState.NOT_FOUND -> throw NotFoundException(this.message)
        ResponseState.INVALID -> throw InvalidRequestException(this.message)
        ResponseState.FORBIDDEN -> throw ForbiddenException(this.message)
    }
}