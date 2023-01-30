package fr.sacane.jmanager.infra.server.adapters

import fr.sacane.jmanager.domain.model.*
import fr.sacane.jmanager.infra.server.entity.*

internal fun Sheet.asResource(): SheetResource {
    val resource = SheetResource()
    resource.isEntry = this.isEntry
    resource.label = this.label
    resource.date = this.date
    resource.amount = this.value
    return resource
}
internal fun Account.asResource(): AccountResource {
    val resource = AccountResource()
    resource.amount = this.amount()
    resource.label = this.label()
    if(this.sheets().isNullOrEmpty()){
        resource.sheets = null
    }else {
        resource.sheets = sheets()?.toMutableList()?.map { model -> model.asResource() }?.toMutableList()
    }
    return resource
}

internal fun User.asResource(): UserResource {
    return UserResource(null, pseudonym, username, password.get(), email, mutableListOf(), this.categories().map { CategoryResource(it.label) }.toMutableList())
}

internal fun SheetResource.toModel(): Sheet{
    return Sheet(this.idSheet!!, this.label!!, this.date!!, this.amount!!, this.isEntry!!)
}
internal fun AccountResource.toModel(): Account{
    return Account(this.idAccount!!, this.amount!!, this.label!!, this.sheets?.map { sheet -> sheet.toModel() }!!.toMutableList())
}
internal fun UserResource.toModel(): User{
    return User(UserId(this.id_user!!), this.username!!, this.email!!, this.pseudonym!!, this.accounts!!.map { account -> account.toModel() }.toMutableList(), Password(this.password!!.toString()), CategoryFactory.allDefaultCategories())
}

internal fun Login.toValidateTicket(user: User): Ticket{
    return Ticket(user, TicketState.AUTHENTICATED, Token(this.id!!, this.lastRefresh!!, this.refreshToken!!))
}