package fr.sacane.jmanager.infrastructure.spi.adapters.utils

import fr.sacane.jmanager.domain.models.*
import fr.sacane.jmanager.domain.models.transaction.Transaction
import fr.sacane.jmanager.domain.models.transaction.regular.FrequencyProperty
import fr.sacane.jmanager.domain.models.transaction.regular.RegularTransactionId
import fr.sacane.jmanager.infrastructure.spi.entity.*
import fr.sacane.jmanager.infrastructure.spi.entity.transaction.ForeverEntity
import fr.sacane.jmanager.infrastructure.spi.entity.transaction.FrequencyPropertyEntity
import fr.sacane.jmanager.infrastructure.spi.entity.transaction.SpecificRepetitionTimesEntity
import fr.sacane.jmanager.infrastructure.spi.entity.transaction.UntilDateEntity
import fr.sacane.jmanager.infrastructure.spi.repositories.DefaultTagPostgresRepository
import fr.sacane.jmanager.infrastructure.spi.repositories.TagPersonalPostgresRepository
import fr.sacane.jmanager.infrastructure.spi.repositories.UserPostgresRepository
import org.springframework.stereotype.Component
import java.awt.Color
import java.time.LocalDateTime

@Component
class BookletMapper(
    val userRepository: UserPostgresRepository,
){
    fun asResource(booklet: Booklet): BookletResource {
        val userResource = booklet.owner?.id?.value?.let { userRepository.findById(it) }
        return if(userResource != null) {
            BookletResource(
                amount = booklet.amount.applyOnValue { it },
                label = booklet.label,
                monthlyPeriodStartDay = booklet.monthlyPeriodStartDay,
                monthlyPeriodEndDay = booklet.monthlyPeriodEndDay,
                transactions = booklet.transactions.map { it.asResource(it.tag?.asResource()) }.toMutableList(),
                owner = userResource.get(),
                initialSold = booklet.initialSold.value,
                idBooklet = booklet.id,
            )
        } else {
            BookletResource(
                amount = booklet.amount.applyOnValue { it },
                label = booklet.label,
                monthlyPeriodStartDay = booklet.monthlyPeriodStartDay,
                monthlyPeriodEndDay = booklet.monthlyPeriodEndDay,
                transactions = booklet.transactions.map { it.asResource(it.tag?.asResource()) }.toMutableList(),
                initialSold = booklet.initialSold.value,
                idBooklet = booklet.id,
            )
        }
    }
}

private fun String.toUuidOrNull(): java.util.UUID? = try {
    java.util.UUID.fromString(this)
} catch (_: IllegalArgumentException) {
    null
}



fun Transaction.asResource(tagResource: AbstractTagResource? = null): TransactionResource {
    val resource = TransactionResource(label=this.label)
    resource.date = this.date
    resource.value = amount.value
    resource.isIncome = isIncome
    resource.idTransaction = this.id
    resource.lastModified = this.lastModified
    resource.regularTransactionId = this.regularTransactionId?.value?.toUuidOrNull()
    if(tagResource != null) {
        when(tagResource) {
            is DefaultTagResource -> resource.tag = tagResource
            is TagPersonalResource -> resource.personalTag = tagResource
        }
    }
    resource.isPreview = isPreview
    return resource
}


fun Booklet.asResource(): BookletResource {
    val transactions = if (this.transactionsSnapshot().isEmpty()) {
        mutableListOf()
    } else {
        transactionsSnapshot().map { it.asResource() }.toMutableList()
    }
    return BookletResource(
        idBooklet = id,
        amount = amount.applyOnValue { it },
        label = label,
        monthlyPeriodStartDay = monthlyPeriodStartDay,
        monthlyPeriodEndDay = monthlyPeriodEndDay,
        transactions = transactions,
        initialSold = this.initialSold.value,
    )
}

fun User.asResource(password: String): UserResource {
    return UserResource(
        username = username,
        password = password,
        email = email,
        booklets = mutableListOf(),
        tags = tags.map { it.toPersonalTag() }.toMutableList(),
        projectionWindowDays = projectionWindowDays,
    )
}



fun TransactionResource.toModel(): Transaction
= Transaction(
    this.idTransaction,
    this.label,
    this.date,
    this.value.toAmount(),
    this.isIncome!!,
    tag = this.tag?.toDomain() ?: this.personalTag?.toDomain() ?: Tag("Aucune", null, Color(0, 0, 0)),
    lastModified = this.lastModified ?: LocalDateTime.now(),
    isPreview = isPreview,
    regularTransactionId = this.regularTransactionId?.let { RegularTransactionId(it.toString()) }
)

fun BookletResource.toModel(): Booklet
= Booklet(
    this.amount.toAmount(),
    this.label,
    this.transactions.map { transaction -> transaction.toModel() }.toMutableList(),
    owner = this.owner?.toModel(),
    initialSold = Amount(this.initialSold),
    id = this.idBooklet,
    monthlyPeriodStartDay = this.monthlyPeriodStartDay,
    monthlyPeriodEndDay = this.monthlyPeriodEndDay,
)


fun UserResource.toModel()
: User = User(
    id = UserId(this.idUser),
    username = this.username,
    email = this.email,
    roles = roles,
    creationDate = creationDate,
    isEnabled = isEnabled,
    projectionWindowDays = projectionWindowDays,
)
fun UserResource.toModelWithSimpleBooklets()
        : User = User(
    id = UserId(this.idUser),
    username = this.username,
    email = this.email,
    booklets = this.booklets.map { it.toSimpleModel() }.toMutableList(),
    projectionWindowDays = projectionWindowDays,
)

fun BookletResource.toSimpleModel(): Booklet = Booklet(
    this.amount.toAmount(),
    this.label,
    id = this.idBooklet,
    monthlyPeriodStartDay = this.monthlyPeriodStartDay,
    monthlyPeriodEndDay = this.monthlyPeriodEndDay,
)

fun UserResource.toModelWithPasswords() : UserWithPassword =
    UserWithPassword(
        User(
            id = UserId(this.idUser),
            username = this.username,
            email = email,
            projectionWindowDays = projectionWindowDays,
        ),
        password,
        roles,
    )

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

fun fr.sacane.jmanager.infrastructure.spi.entity.Color.asAwtColor(): Color = Color(this.red, this.green, this.blue)

fun Tag.toPersonalTag(userResource: UserResource? = null): TagPersonalResource{
    return TagPersonalResource(this.id, this.label, fr.sacane.jmanager.infrastructure.spi.entity.Color(color.red, color.green, color.blue), userResource)
}

fun User.asExistingResource(): UserResource
        = UserResource(idUser = this.id.value,
    username = username,
    email = email,
    booklets = this.booklets.map {it.asResource()}.toMutableList(),
    tags = this.tags.map { it.toPersonalTag() }.toMutableList(),
    projectionWindowDays = this.projectionWindowDays,
)

@Component
class JpaTagMapperAdapter(
    private val defaultTagPostgresRepository: DefaultTagPostgresRepository,
    private val tagPersonalPostgresRepository: TagPersonalPostgresRepository
) {
    fun mapToResource(tag: Tag?): AbstractTagResource? {
        return tag?.id?.let {
            if(tag.isDefault) {
                defaultTagPostgresRepository.findByIdNullable(it)
            } else {
                tagPersonalPostgresRepository.findByIdNullable(it)
            }
        }
    }
}

fun FrequencyProperty.toResource(): FrequencyPropertyEntity {
    return when(this) {
        is FrequencyProperty.Forever -> ForeverEntity()
        is FrequencyProperty.UntilDate -> UntilDateEntity(this.date)
        is FrequencyProperty.SpecificRepetitionTimes -> SpecificRepetitionTimesEntity(this.number)
    }
}