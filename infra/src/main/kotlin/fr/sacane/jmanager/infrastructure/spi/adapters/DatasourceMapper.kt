package fr.sacane.jmanager.infrastructure.spi.adapters

import fr.sacane.jmanager.domain.models.*
import fr.sacane.jmanager.domain.port.spi.TagRepository
import fr.sacane.jmanager.infrastructure.rest.asAwtColor
import fr.sacane.jmanager.infrastructure.spi.entity.*
import fr.sacane.jmanager.infrastructure.spi.repositories.UserPostgresRepository
import org.springframework.stereotype.Component
import java.awt.Color
import java.time.LocalDateTime

@Component
class AccountMapper(
    val userRepository: UserPostgresRepository,
    val tagRepository: TagRepository
){
    fun asResource(account: Account): AccountResource {
        val userResource = account.owner?.id?.id?.let { userRepository.findById(it) }
        return if(userResource != null) {
            AccountResource(amount = account.amount.applyOnValue { it }, label = account.label, sheets = account.transactions.map { it.asResource(it.tag.asResource()) }.toMutableList(), userResource.get(), initialSold = account.initialSold.amount, idAccount = account.id, previewAmount = account.previewAmount.amount)
        } else {
            AccountResource(amount = account.amount.applyOnValue { it }, label = account.label, sheets = account.transactions.map { it.asResource(it.tag.asResource()) }.toMutableList(), initialSold = account.initialSold.amount, idAccount = account.id, previewAmount = account.previewAmount.amount)
        }
    }
}



internal fun Transaction.asResource(tagResource: AbstractTagResource? = null): TransactionResource {
    val resource = TransactionResource()
    resource.label = this.label
    resource.date = this.date
    this.exportAmountValues { expense, isIncome ->
        resource.value = expense
        resource.isIncome = isIncome
    }
    resource.idSheet = this.id
    resource.lastModified = this.lastModified
    if(tagResource != null) {
        when(tagResource) {
            is DefaultTagResource -> resource.tag = tagResource
            is TagPersonalResource -> resource.personalTag = tagResource
        }
    }
    resource.isPreview = isPreview
    return resource
}
internal fun Account.asResource(): AccountResource {
    val sheets = if (this.sheets().isEmpty()) {
        mutableListOf()
    } else {
        sheets().map { it.asResource() }.toMutableList()
    }
    return AccountResource(idAccount = id, amount = amount.applyOnValue { it }, label = label, sheets = sheets, initialSold = this.initialSold.amount, previewAmount = this.previewAmount.amount)
}

internal fun User.asResource(password: Password): UserResource {
    return UserResource(username = username, password = password.get(), email = email, mutableListOf(), tags = tags.map { it.toPersonalTag() }.toMutableList())
}

internal fun User.asExistingResource(): UserResource
    = UserResource(idUser = this.id.id,
        username = username,
        email = email,
        accounts = this.accounts.map {it.asResource()}.toMutableList(),
        tags = this.tags.map { it.toPersonalTag() }.toMutableList()
    )

internal fun TransactionResource.toModel(): Transaction
= Transaction(
    this.idSheet,
    this.label,
    this.date,
    this.value.toAmount(),
    this.isIncome!!,
    tag = this.tag?.toDomain() ?: this.personalTag?.toDomain() ?: Tag("Aucune", null, Color(0, 0, 0)),
    lastModified = this.lastModified ?: LocalDateTime.now(),
    isPreview = isPreview
)

internal fun AccountResource.toModel(): Account
= Account(
    this.idAccount,
    this.amount.toAmount(),
    this.label,
    this.sheets.map { sheet -> sheet.toModel() }.toMutableList(),
    this.owner?.toModel(),
    previewAmount = this.previewAmount.toAmount(),
    initialSold = Amount(this.initialSold)
)


internal fun UserResource.toModel()
: User = User(
    id = UserId(this.idUser),
    username = this.username,
    email = this.email,
)
internal fun UserResource.toModelWithSimpleAccounts()
        : User = User(
    id = UserId(this.idUser),
    username = this.username,
    email = this.email,
    accounts_ = this.accounts.map { account -> account.toSimpleModel() }.toMutableList(),
)

internal fun AccountResource.toSimpleModel(): Account = Account(this.idAccount, this.amount.toAmount(), this.label, previewAmount = this.previewAmount.toAmount())

internal fun UserResource.toMinimalUserRepresentation()
: MinimalUserRepresentation = MinimalUserRepresentation(
    UserId(this.idUser),
    this.username,
    this.email
)

internal fun UserResource.toModelWithPasswords() : UserWithPassword =
    UserWithPassword(User(id = UserId(this.idUser), username = this.username, email = this.email), Password.fromBytes(this.password))

fun Tag.asResource(): AbstractTagResource {
    return when(this.isDefault) {
        true -> DefaultTagResource(this.id, this.label, fr.sacane.jmanager.infrastructure.spi.entity.Color(this.color.red, this.color.green, this.color.blue))
        false -> TagPersonalResource(this.id, this.label, fr.sacane.jmanager.infrastructure.spi.entity.Color(this.color.red, this.color.green, this.color.blue))
    }
}
fun AbstractTagResource.toDomain(): Tag {
    return when(this) {
        is DefaultTagResource -> Tag(this.name, this.idTag, this.color.asAwtColor(), true)
        is TagPersonalResource -> Tag(this.name, this.idTag, this.color.asAwtColor(), false)
    }
}

fun Tag.toPersonalTag(userResource: UserResource? = null): TagPersonalResource{
    return TagPersonalResource(this.id, this.label, fr.sacane.jmanager.infrastructure.spi.entity.Color(color.red, color.green, color.blue), userResource)
}