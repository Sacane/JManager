package fr.sacane.jmanager.infrastructure.server.adapters

import fr.sacane.jmanager.domain.models.*
import fr.sacane.jmanager.infrastructure.server.entity.*


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
    return UserResource(null, username, password.get(), email, mutableListOf(), categories().map { CategoryResource(it.label) }.toMutableList())
}

internal fun SheetResource.toModel(): Sheet{
    return Sheet(this.idSheet!!, this.label!!, this.date!!, this.amount!!, this.isEntry!!)
}
internal fun AccountResource.toModel(): Account{
    return Account(this.idAccount!!, this.amount!!, this.label!!, this.sheets?.map { sheet -> sheet.toModel() }!!.toMutableList())
}
internal fun UserResource.toModel(): User{
    return User(
        UserId(this.idUser!!),
        this.username!!,
        this.email!!,
        this.accounts!!.map { account -> account.toModel() }.toMutableList(),
        Password.fromBytes(this.password!!),
        CategoryFactory.allDefaultCategories()
    )
}


internal fun Login.toModel(): Token{
    return Token(this.token!!, this.lastRefresh!!, this.refreshToken!!)
}