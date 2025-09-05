package fr.sacane.jmanager.infrastructure.spi.adapters

import fr.sacane.jmanager.domain.models.*
import fr.sacane.jmanager.domain.models.transaction.regular.RegularTransaction
import fr.sacane.jmanager.domain.models.transaction.regular.RegularTransactionId
import fr.sacane.jmanager.domain.models.transaction.Transaction
import fr.sacane.jmanager.domain.models.transaction.regular.FrequencyProperty
import fr.sacane.jmanager.domain.models.transaction.regular.MonthlyTransaction
import fr.sacane.jmanager.domain.port.spi.TagRepository
import fr.sacane.jmanager.infrastructure.api.asAwtColor
import fr.sacane.jmanager.infrastructure.spi.entity.*
import fr.sacane.jmanager.infrastructure.spi.entity.transaction.AbstractRegularTransactionResource
import fr.sacane.jmanager.infrastructure.spi.entity.transaction.ForeverEntity
import fr.sacane.jmanager.infrastructure.spi.entity.transaction.FrequencyPropertyEntity
import fr.sacane.jmanager.infrastructure.spi.entity.transaction.MonthlyRegularRegularTransactionEntity
import fr.sacane.jmanager.infrastructure.spi.entity.transaction.SpecificRepetitionTimesEntity
import fr.sacane.jmanager.infrastructure.spi.entity.transaction.UntilDateEntity
import fr.sacane.jmanager.infrastructure.spi.repositories.DefaultTagPostgresRepository
import fr.sacane.jmanager.infrastructure.spi.repositories.MonthlyTransactionResourceJpaRepository
import fr.sacane.jmanager.infrastructure.spi.repositories.TagPersonalPostgresRepository
import fr.sacane.jmanager.infrastructure.spi.repositories.UserPostgresRepository
import jakarta.transaction.Transactional
import org.springframework.stereotype.Component
import java.awt.Color
import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.UUID

@Component
class AccountMapper(
    val userRepository: UserPostgresRepository,
    val tagRepository: TagRepository
){
    fun asResource(booklet: Booklet): AccountResource {
        val userResource = booklet.owner?.id?.value?.let { userRepository.findById(it) }
        return if(userResource != null) {
            AccountResource(amount = booklet.amount.applyOnValue { it }, label = booklet.label, sheets = booklet.transactions.map { it.asResource(it.tag.asResource()) }.toMutableList(), userResource.get(),  initialSold = booklet.initialSold.amount, idAccount = booklet.id, previewAmount = booklet.previewAmount.amount)
        } else {
            AccountResource(amount = booklet.amount.applyOnValue { it }, label = booklet.label, sheets = booklet.transactions.map { it.asResource(it.tag.asResource()) }.toMutableList(), initialSold = booklet.initialSold.amount, idAccount = booklet.id, previewAmount = booklet.previewAmount.amount)
        }
    }
}



internal fun Transaction.asResource(tagResource: AbstractTagResource? = null): TransactionResource {
    val resource = TransactionResource(label=this.label)
    resource.date = this.date
    resource.value = amount.amount
    resource.isIncome = isIncome
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


internal fun Booklet.asResource(): AccountResource {
    val sheets = if (this.sheets().isEmpty()) {
        mutableListOf()
    } else {
        sheets().map { it.asResource() }.toMutableList()
    }
    return AccountResource(idAccount = id, amount = amount.applyOnValue { it }, label = label, sheets = sheets, initialSold = this.initialSold.amount, previewAmount = this.previewAmount.amount)
}

internal fun User.asResource(password: String): UserResource {
    return UserResource(username = username, password = password, email = email, mutableListOf(), tags = tags.map { it.toPersonalTag() }.toMutableList())
}



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

internal fun AccountResource.toModel(): Booklet
= Booklet(
    this.amount.toAmount(),
    this.label,
    this.sheets.map { sheet -> sheet.toModel() }.toMutableList(),
    this.owner?.toModel(),
    previewAmount = this.previewAmount.toAmount(),
    initialSold = Amount(this.initialSold),
    id = this.idAccount
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
    booklets = this.accounts.map { account -> account.toSimpleModel() }.toMutableList(),
)

internal fun AccountResource.toSimpleModel(): Booklet = Booklet(this.amount.toAmount(), this.label, previewAmount = this.previewAmount.toAmount(), id = this.idAccount)

internal fun UserResource.toModelWithPasswords() : UserWithPassword =
    UserWithPassword(User(id = UserId(this.idUser), username = this.username, email = email), password)

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

internal fun User.asExistingResource(): UserResource
        = UserResource(idUser = this.id.value,
    username = username,
    email = email,
    accounts = this.booklets.map {it.asResource()}.toMutableList(),
    tags = this.tags.map { it.toPersonalTag() }.toMutableList()
)

internal fun RegularTransactionResource.toDomain(): RegularTransaction {
    return MonthlyTransaction(
        label = this.label,
        amount = Amount(this.amount),
        isIncome = this.isIncome,
        id = RegularTransactionId(this.regularTransactionId.toString()),
        this.startDate,
        tag = this.tag?.toDomain() ?: this.personalTag?.toDomain() ?: Tag("Aucune", null, Color(0, 0, 0)),
        frequencyProperty = FrequencyProperty.Forever(),
    )
}

@Component
class RegularTransactionOperatorAdapter(
    private val monthlyTransactionResourceJpaRepository: MonthlyTransactionResourceJpaRepository,
    private val tagMapperAdapter: JpaTagMapperAdapter,
    private val defaultTagPostgresRepository: DefaultTagPostgresRepository
) {

    @Transactional
    fun save(user: UserResource, regularTransaction: RegularTransaction): AbstractRegularTransactionResource {
        return when (regularTransaction) {
            is MonthlyTransaction -> {
                val monthlyRegularTransactionEntity = MonthlyRegularRegularTransactionEntity(
                    id = UUID.fromString(regularTransaction.id.value),
                    startDate = regularTransaction.startDate,
                    label = regularTransaction.label,
                    amount = regularTransaction.amount.amount.toDouble(),
                    isIncome = regularTransaction.isIncome,
                    frequencyProperty = regularTransaction.frequencyProperty.toResource(),
                ).copy(owner = user)
                when(val tagResource = tagMapperAdapter.mapToResource(
                    regularTransaction.tag
                )) {
                    is DefaultTagResource -> monthlyRegularTransactionEntity.copy(
                        tag = tagResource,
                        personalTag = null
                    )
                    is TagPersonalResource -> monthlyRegularTransactionEntity.copy(
                        tag = null,
                        personalTag = tagResource
                    )
                    null -> monthlyRegularTransactionEntity.copy(
                        tag = defaultTagPostgresRepository.findUnknownTag(),
                        personalTag = null
                    )
                }
                monthlyTransactionResourceJpaRepository.save(monthlyRegularTransactionEntity)
            }
        }
    }
}

@Component
class JpaTagMapperAdapter(
    private val defaultTagPostgresRepository: DefaultTagPostgresRepository,
    private val tagPersonalPostgresRepository: TagPersonalPostgresRepository
) {
    fun mapToResource(tag: Tag): AbstractTagResource? {
        return tag.id?.let {
            if(tag.isDefault) {
                defaultTagPostgresRepository.findByIdNullable(it)
            } else {
                tagPersonalPostgresRepository.findByIdNullable(it)
            }
        }
    }
}

internal fun FrequencyProperty.toResource(): FrequencyPropertyEntity {
    return when(this) {
        is FrequencyProperty.Forever -> ForeverEntity()
        is FrequencyProperty.UntilDate -> UntilDateEntity(this.date)
        is FrequencyProperty.SpecificRepetitionTimes -> SpecificRepetitionTimesEntity(this.number)
    }
}

internal fun FrequencyPropertyEntity.toDomain(): FrequencyProperty {
    return when(this) {
        is ForeverEntity -> FrequencyProperty.Forever()
        is UntilDateEntity -> FrequencyProperty.UntilDate(this.date)
        is SpecificRepetitionTimesEntity -> FrequencyProperty.SpecificRepetitionTimes(this.number)
        else -> throw IllegalArgumentException("Unknown FrequencyPropertyEntity type")
    }
}

internal fun AbstractRegularTransactionResource.toDomain() = when(this) {
    is MonthlyRegularRegularTransactionEntity -> MonthlyTransaction(
        label = this.label,
        amount = Amount(BigDecimal(this.amount)),
        isIncome = this.isIncome,
        id = RegularTransactionId(this.id.toString()),
        this.startDate,
        tag = this.tag?.toDomain() ?: this.personalTag?.toDomain()!!,
        frequencyProperty = this.frequencyProperty.toDomain()
    )

    else -> TODO("Not yet implemented")
}