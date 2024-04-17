package fr.sacane.jmanager.infrastructure.spi.adapters

import fr.sacane.jmanager.domain.models.*
import fr.sacane.jmanager.infrastructure.spi.entity.*
import fr.sacane.jmanager.infrastructure.spi.repositories.UserPostgresRepository
import org.springframework.stereotype.Component
import java.awt.Color

@Component
class AccountMapper(val userRepository: UserPostgresRepository){
    fun asResource(account: Account): AccountResource {
        val userResource = account.owner?.id?.id?.let { userRepository.findById(it) }
        return if(userResource != null) {
            AccountResource(amount = account.sold.applyOnValue { it }, label = account.label, sheets = account.transactions.map { it.asResource() }.toMutableList(), userResource.get(), idAccount = account.id)
        } else {
            AccountResource(amount = account.sold.applyOnValue { it }, label = account.label, sheets = account.transactions.map { it.asResource() }.toMutableList(), idAccount = account.id)
        }
    }
}

internal fun Transaction.asResource(tagResource: TagResource? = null): SheetResource {
    val resource = SheetResource()
    resource.label = this.label
    resource.date = this.date
    this.exportAmountValues { expense, income, sold ->
        resource.expenses = expense
        resource.income = income
        resource.accountAmount = sold
    }
    resource.idSheet = this.id
    resource.position = this.position
    if(tagResource != null) {
        resource.tag = tagResource
    }
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
    return UserResource(username = username, password = password.get(), email = email, mutableListOf(), tags = tags.map { TagResource(idTag = it.id, name = it.label) }.toMutableList())
}

internal fun User.asExistingResource(): UserResource
    = UserResource(idUser = this.id.id,
        username = username,
        password = password.get(),
        email = email,
        accounts = this.accounts.map {it.asResource()}.toMutableList(),
        tags = this.tags.map { it.asResource() }.toMutableList()
    )

internal fun Tag.asResource(): TagResource = TagResource(this.id, this.label)
internal fun SheetResource.toModel(): Transaction{
    return Transaction(this.idSheet,
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
        this.sheets.map { sheet -> sheet.toModel() }.toMutableList(),
        this.owner?.toModel())
}

internal fun UserResource.toModel()
: User = User(
    id = UserId(this.idUser),
    username = this.username,
    email = this.email,
    password = Password.fromBytes(this.password)
)
internal fun UserResource.toModelWithSimpleAccounts()
        : User = User(
    id = UserId(this.idUser),
    username = this.username,
    email = this.email,
    accounts_ = this.accounts.map { account -> account.toSimpleModel() }.toMutableList(),
    password = Password.fromBytes(this.password)
)

internal fun AccountResource.toSimpleModel(): Account = Account(this.idAccount, this.amount.toAmount(), this.label)

internal fun UserResource.toMinimalUserRepresentation()
: MinimalUserRepresentation = MinimalUserRepresentation(
    UserId(this.idUser),
    this.username,
    this.email
)

internal fun UserResource.toModelWithPasswords() : User =
    User(id = UserId(this.idUser), username = this.username, email = this.email, password = Password.fromBytes(this.password))
internal fun TagResource.toModel(): Tag = Tag(this.name, this.idTag, color = Color(this.color.red, this.color.green, this.color.blue), isDefault = this.isDefault)

internal fun Login.toModel()
: AccessToken = AccessToken(this.token, this.tokenLifeTime, this.refreshToken, this.refreshTokenLifetime)

internal fun Tag.toEntity()
: TagResource = TagResource(name = this.label, color = fr.sacane.jmanager.infrastructure.spi.entity.Color(this.color.red, this.color.green, this.color.blue), isDefault = this.isDefault)
