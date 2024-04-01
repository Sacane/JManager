package fr.sacane.jmanager.infrastructure.spi.adapters

import fr.sacane.jmanager.domain.models.*
import fr.sacane.jmanager.infrastructure.spi.entity.*


internal fun Sheet.asResource(): SheetResource {
    val resource = SheetResource()
    resource.label = this.label
    resource.date = this.date
    this.exportAmountValues { expense, income, sold ->
        resource.expenses = expense
        resource.income = income
        resource.accountAmount = sold
    }
    resource.category = resource.category
    resource.idSheet = this.id
    resource.position = this.position
    return resource
}
internal fun Account.asResource(): AccountResource {
    val sheets = if (this.sheets().isEmpty()) {
        mutableListOf()
    } else {
        sheets().map { it.asResource() }.toMutableList()
    }
    return AccountResource(idAccount = id, amount = sold.applyOnValue { it }, label = label, sheets = sheets)
}

internal fun User.asResource(): UserResource {
    return UserResource(username, password.get(), email, mutableListOf(), distinctCategories.map { CategoryResource(label = it.label) }.toMutableList())
}

internal fun User.asExistingResource(): UserResource {
    return UserResource(idUser = this.id.id,
        username = username,
        password = password.get(),
        email = email,
        accounts = this.accounts.map {it.asResource()}.toMutableList(),
        tags = this.distinctCategories.map { it.asResource() }.toMutableList()
    )
}
internal fun Tag.asResource(): TagResource = TagResource(this.id, this.label)
internal fun SheetResource.toModel(): Sheet{
    return Sheet(this.idSheet,
        this.label,
        this.date,
        this.expenses.toAmount(),
        this.income.toAmount(),
        this.accountAmount.toAmount(),
        position=this.position)
}
internal fun AccountResource.toModel(): Account{
    return Account(
        this.idAccount,
        this.amount.toAmount(),
        this.label,
        this.sheets.map { sheet -> sheet.toModel() }.toMutableList())
}

internal fun UserResource.toModel()
: User = User(
    UserId(this.idUser),
    this.username,
    this.email,
    this.accounts.map { account -> account.toModel() }.toMutableList(),
    Password.fromBytes(this.password),
    this.tags.map { it.toModel() }.toMutableList()
)

internal fun TagResource.toModel(): Tag = Tag(this.name, this.idTag)

internal fun Login.toModel()
: AccessToken = AccessToken(this.token, this.tokenLifeTime, this.refreshToken, this.refreshTokenLifetime)
