package fr.sacane.jmanager.infrastructure.api.adapters

import fr.sacane.jmanager.domain.models.*
import fr.sacane.jmanager.infrastructure.api.*
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import java.util.*

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
internal fun AccountDTO.toModel(): Account {
    return Account(this.id, this.amount, this.labelAccount, this.sheets?.map { it.toModel() }?.toMutableList())
}
internal fun RegisteredUserDTO.toModel(): User {
    return User(UserId(0), this.username, this.email, mutableListOf(), Password(this.password), mutableListOf(
        CategoryFactory.DEFAULT_CATEGORY))
}

internal fun Sheet.toDTO(): SheetDTO {
    return SheetDTO(this.id, this.label, this.value, this.isEntry, this.date)
}

internal fun User.toDTO(): UserDTO {
    return UserDTO(this.id.get(), this.username, this.email)
}
internal fun Long.id(): UserId {
    return UserId(this)
}
internal fun <T> Response<T>.toResponseEntity(): ResponseEntity<T>{
    return when(this.status){
        ResponseState.OK -> ResponseEntity(this.get(), HttpStatus.OK)
        ResponseState.TIMEOUT, ResponseState.NOT_FOUND -> ResponseEntity.notFound().build()
        ResponseState.INVALID -> ResponseEntity.badRequest().build()
    }
}

internal fun TokenDTO.toToken(): Token {
    return Token(UUID.fromString(this.token), null, UUID.fromString(this.refreshToken))
}

internal fun Token.toDTO(): TokenDTO {
    return TokenDTO(this.id.toString(), this.refreshToken.toString())
}

//internal fun UserCredentialsDTO.toDataModel(): CredData{
//    return CredData(
//        this.id.id(),
//        Password(this.password)
//    )
//}
//data class CredData (val user: UserId, val password: Password, val token: Token)