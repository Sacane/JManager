package fr.sacane.jmanager.infra.api

import fr.sacane.jmanager.domain.model.*

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
internal fun Long.id(): UserId{
    return UserId(this)
}
