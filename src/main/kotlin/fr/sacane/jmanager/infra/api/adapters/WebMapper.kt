package fr.sacane.jmanager.infra.api.adapters

import fr.sacane.jmanager.domain.model.*
import fr.sacane.jmanager.infra.api.AccountDTO
import fr.sacane.jmanager.infra.api.RegisteredUserDTO
import fr.sacane.jmanager.infra.api.SheetDTO
import fr.sacane.jmanager.infra.api.UserDTO
import org.springframework.http.HttpEntity
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import java.net.http.HttpResponse

internal fun Account.toDTO(): AccountDTO {
    return AccountDTO(
        this.id(),
        this.amount(),
        this.label(),
        this.sheets()?.map { sheet -> sheet.toDTO() }
    )
}

internal fun SheetDTO.toModel(): Sheet {
    return Sheet(0, this.label, this.date, this.amount, this.action)
}
internal fun AccountDTO.toModel(): Account{
    return Account(this.id, this.amount, this.labelAccount, this.sheets?.map { it.toModel() }?.toMutableList())
}
internal fun RegisteredUserDTO.toModel(): User {
    return User(this.id.id(), this.username, this.email, this.pseudonym, mutableListOf(), Password(this.password), mutableListOf(
        CategoryFactory.DEFAULT_CATEGORY))
}

internal fun Sheet.toDTO(): SheetDTO {
    return SheetDTO(this.label, this.value, this.isEntry, this.date)
}

internal fun User.toDTO(): UserDTO {
    return UserDTO(this.id.get(), this.username, this.pseudonym, this.email)
}
internal fun Long.id(): UserId {
    return UserId(this)
}
internal fun <T> Response<T>.toResponseEntity(): ResponseEntity<T>{
    return when(this.status){
        TicketState.OK -> ResponseEntity(this.get(), HttpStatus.OK)
        TicketState.TIMEOUT -> ResponseEntity(this.get(), HttpStatus.UNAUTHORIZED)
        TicketState.INVALID -> ResponseEntity(this.get(), HttpStatus.BAD_REQUEST)
    }
}