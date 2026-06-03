package fr.sacane.jmanager.application.api

import fr.sacane.jmanager.domain.models.*
import fr.sacane.jmanager.domain.models.transaction.regular.RegularTransaction
import fr.sacane.jmanager.domain.models.transaction.Transaction
import fr.sacane.jmanager.domain.utils.ErrorCatalog
import fr.sacane.jmanager.domain.utils.Result
import fr.sacane.jmanager.domain.utils.ResultState
import fr.sacane.jmanager.application.api.booklet.BookletDTO
import fr.sacane.jmanager.application.api.session.UserDTO
import fr.sacane.jmanager.application.api.tag.ColorDTO
import fr.sacane.jmanager.application.api.tag.TagDTO
import fr.sacane.jmanager.application.api.transaction.RegularTransactionDTO
import fr.sacane.jmanager.application.api.transaction.TransactionResult
import fr.sacane.jmanager.application.api.transaction.toDTO
import org.springframework.http.ResponseEntity
import java.awt.Color

internal fun Booklet.toDTO(): BookletDTO = BookletDTO(
    this.id?.toString() ?: throw InternalServerErrorException(ErrorCatalog.INTERNAL_UNEXPECTED, "Impossible d'envoyer null au client"),
    this.amount.value,
    this.label,
    this.transactionsSnapshot().map { transaction -> transaction.toDTO() },
    this.amount.currency.symbol
)

internal fun TransactionResult.toModel(): Transaction {
    val tag: Tag? = if (tagDTO == null) {
        Tag.Default("Aucune")
    } else {
        tagDTO.toDomain()
    }
    return Transaction(
        this.id?.let { java.util.UUID.fromString(it) },
        this.label,
        this.date,
        Amount(this.value),
        this.isIncome,
        tag = tag,
        isPreview = isPreview,
        regularTransactionId = this.regularTransactionId?.let { fr.sacane.jmanager.domain.models.transaction.regular.RegularTransactionId(it) }
    )
}

internal fun Transaction.toDTO(): TransactionResult {
    return TransactionResult(id?.toString(), label, amount.value, amount.currency.symbol, isIncome, date, tagDTO = tag?.toDTO(), isPreview, regularTransactionId = regularTransactionId?.value)
}


internal fun User.toDTO(): UserDTO = UserDTO(
    id = this.id.value?.toString() ?: "",
    username = this.username,
    email = this.email,
    createdDate = creationDate.toString(),
    roles = this.roles.map { it.name },
    subscriptionPlan = this.subscriptionPlan.name,
)

internal fun UserForAdmin.toDTO(): UserDTO = UserDTO(
    id = this.user.id.value?.toString() ?: "",
    username = this.user.username,
    email = this.user.email,
    createdDate = this.createdDate.toString(),
    subscriptionPlan = this.user.subscriptionPlan.name,
)

internal fun String.id(): UserId = UserId(java.util.UUID.fromString(this))

internal fun <T> Result<T>.toHttpResponse()
: ResponseEntity<T> = when(this.status){
    ResultState.OK -> mapNullable { ResponseEntity.ok(it) }
    ResultState.NOT_FOUND,
    ResultState.FEATURE_DISABLED,
    ResultState.TAG_NOT_FOUND,
    ResultState.USER_NOT_FOUND,
    ResultState.BOOKLET_NOT_FOUND, ResultState.TRANSACTION_NOT_FOUND,
    ResultState.TAG_PLACEHOLDER_UNDEFINED,
    ResultState.BOOKLET_LABEL_NOT_EXIST -> throw NotFoundException(
        this.errorInfo?.code ?: this.status.code,
        this.errorInfo?.detail ?: this.message,
        this.errorInfo?.key,
    )
    ResultState.INVALID, ResultState.REGISTRATION_ERROR,
    ResultState.BOOKLET_LABEL_EXIST,
    ResultState.TRANSACTION_ENTRY_ERROR,
    ResultState.TAG_SHOULD_NOT_BE_DEFAULT,
    ResultState.BAD_REQUEST, ResultState.INFRASTRUCTURE_ERROR,
    ResultState.BOOKLET_MAXIMUM_SIZE_REACHED,
    ResultState.TAG_PARENT_IS_SUBTAG,
    ResultState.PASSWORD_UNCHANGED -> throw InvalidRequestException(
        this.errorInfo?.code ?: this.status.code,
        this.errorInfo?.detail ?: this.message,
        this.errorInfo?.key,
    )
    ResultState.TAG_LABEL_ALREADY_TAKEN, ResultState.FORBIDDEN, ResultState.USER_UNAUTHORIZED -> throw ForbiddenException(
        this.errorInfo?.code ?: this.status.code,
        this.errorInfo?.detail ?: this.message,
        this.errorInfo?.key,
    )
    ResultState.TAG_IN_USE,
    ResultState.EMAIL_ALREADY_VERIFIED -> throw ConflictException(
        this.errorInfo?.code ?: this.status.code,
        this.errorInfo?.detail ?: this.message,
        this.errorInfo?.key,
    )
    ResultState.EMAIL_VERIFICATION_TOKEN_EXPIRED -> throw GoneException(
        this.errorInfo?.code ?: this.status.code,
        this.errorInfo?.detail ?: this.message,
        this.errorInfo?.key,
    )
    ResultState.TIMEOUT  -> throw TimeOutException(
        this.errorInfo?.code ?: this.status.code,
        this.errorInfo?.detail ?: this.message,
        this.errorInfo?.key,
    )
    ResultState.UNAUTHORIZED, ResultState.USER_NOT_AUTHENTICATED,
         ResultState.PASSWORD_NOT_MATCH -> throw UnauthorizedRequestException(
        this.errorInfo?.code ?: this.status.code,
        this.errorInfo?.detail ?: this.message,
        this.errorInfo?.key,
    )
    ResultState.INTERNAL_SERVER_ERROR -> throw InternalServerErrorException(
        this.errorInfo?.code ?: this.status.code,
        this.errorInfo?.detail ?: this.message,
        this.errorInfo?.key,
    )
}

internal fun fr.sacane.jmanager.infrastructure.spi.entity.Color.asAwtColor(): Color = Color(this.red, this.green, this.blue)
internal fun ColorDTO.asAwtColor(): Color = Color(this.red, this.green, this.blue)

internal fun Color.toDTO(): ColorDTO = ColorDTO(this.red, this.green, this.blue)

internal fun Tag.toDTO(): TagDTO = TagDTO(
    tagId = this.id?.toString(),
    label = this.label,
    isDefault = this.isDefault,
    colorDTO = this.color.toDTO(),
    parentId = (this as? Tag.Personal)?.parentId?.toString()
)

internal fun TagDTO.toDomain(): Tag {
    val id = this.tagId?.let { java.util.UUID.fromString(it) }
    val color = Color(this.colorDTO.red, this.colorDTO.green, this.colorDTO.blue)
    return if (this.isDefault) Tag.Default(label = this.label, id = id, color = color)
    else Tag.Personal(label = this.label, id = id, color = color, parentId = this.parentId?.let { java.util.UUID.fromString(it) })
}

internal fun RegularTransaction.toDTO(): RegularTransactionDTO {
    val regularityType = when (this.recurrenceRule) {
        is fr.sacane.jmanager.domain.models.transaction.regular.RecurrenceRule.Monthly -> "MONTHLY"
        is fr.sacane.jmanager.domain.models.transaction.regular.RecurrenceRule.Yearly -> "YEARLY"
        is fr.sacane.jmanager.domain.models.transaction.regular.RecurrenceRule.Weekly -> "WEEKLY"
        is fr.sacane.jmanager.domain.models.transaction.regular.RecurrenceRule.Daily -> "DAILY"
    }

    return RegularTransactionDTO(
        id = this.id.value,
        label = this.label,
        startDate = this.startDate,
        value = this.amount.value,
        isIncome = this.isIncome,
        regularity = regularityType,
        tagDTO = this.tag!!.toDTO(),
        frequencyProperty = frequencyProperty.toDTO(),
        bookletIds = this.associatedBooklets.mapNotNull { it.id?.toString() }
    )
}