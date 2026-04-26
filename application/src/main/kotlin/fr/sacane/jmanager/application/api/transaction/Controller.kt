package fr.sacane.jmanager.application.api.transaction

import fr.sacane.jmanager.domain.hexadoc.Adapter
import fr.sacane.jmanager.domain.hexadoc.Side
import fr.sacane.jmanager.domain.models.Page
import fr.sacane.jmanager.domain.models.TransactionResumeResult
import fr.sacane.jmanager.domain.models.toAmount
import fr.sacane.jmanager.domain.models.transaction.regular.RecurrenceRule
import fr.sacane.jmanager.domain.models.transaction.regular.RegularTransaction
import fr.sacane.jmanager.domain.models.transaction.regular.RegularTransactionId
import fr.sacane.jmanager.domain.port.input.booklet.LoadTransactionsForBookletForAMonthQuery
import fr.sacane.jmanager.domain.port.input.booklet.LoadTransactionsForBookletForAMonthUseCase
import fr.sacane.jmanager.domain.port.input.regularTransaction.BookRegularTransactionCommand
import fr.sacane.jmanager.domain.port.input.regularTransaction.BookRegularTransactionUseCase
import fr.sacane.jmanager.domain.port.input.regularTransaction.DeleteRegularTransactionCommand
import fr.sacane.jmanager.domain.port.input.regularTransaction.DeleteRegularTransactionUseCase
import fr.sacane.jmanager.domain.port.input.regularTransaction.DeleteRegularTransactionsCommand
import fr.sacane.jmanager.domain.port.input.regularTransaction.DeleteRegularTransactionsUseCase
import fr.sacane.jmanager.domain.port.input.regularTransaction.GetAllRegularTransactionsQuery
import fr.sacane.jmanager.domain.port.input.regularTransaction.GetAllRegularTransactionsUseCase
import fr.sacane.jmanager.domain.port.input.regularTransaction.GetRegularTransactionByIdQuery
import fr.sacane.jmanager.domain.port.input.regularTransaction.GetRegularTransactionByIdUseCase
import fr.sacane.jmanager.domain.port.input.regularTransaction.LinkRegularTransactionToBookletCommand
import fr.sacane.jmanager.domain.port.input.regularTransaction.LinkRegularTransactionToBookletUseCase
import fr.sacane.jmanager.domain.port.input.regularTransaction.UnlinkRegularTransactionFromBookletCommand
import fr.sacane.jmanager.domain.port.input.regularTransaction.UnlinkRegularTransactionFromBookletUseCase
import fr.sacane.jmanager.domain.port.input.regularTransaction.UpdateRegularTransactionCommand
import fr.sacane.jmanager.domain.port.input.regularTransaction.UpdateRegularTransactionUseCase
import fr.sacane.jmanager.domain.models.SessionToken
import fr.sacane.jmanager.domain.port.api.TransactionDeletionResult
import fr.sacane.jmanager.domain.port.input.transaction.BookTransactionCommand
import fr.sacane.jmanager.domain.port.input.transaction.BookTransactionUseCase
import fr.sacane.jmanager.domain.port.input.transaction.ConfirmPreviewTransactionCommand
import fr.sacane.jmanager.domain.port.input.transaction.ConfirmPreviewTransactionUseCase
import fr.sacane.jmanager.domain.port.input.transaction.DeleteTransactionsByIdsCommand
import fr.sacane.jmanager.domain.port.input.transaction.DeleteTransactionsByIdsUseCase
import fr.sacane.jmanager.domain.port.input.transaction.EditTransactionCommand
import fr.sacane.jmanager.domain.port.input.transaction.EditTransactionUseCase
import fr.sacane.jmanager.domain.port.input.transaction.FindTransactionByIdQuery
import fr.sacane.jmanager.domain.port.input.transaction.FindTransactionByIdUseCase
import fr.sacane.jmanager.domain.utils.ResultState
import fr.sacane.jmanager.domain.toUUID
import fr.sacane.jmanager.domain.toUUIDs
import fr.sacane.jmanager.application.api.*
import fr.sacane.jmanager.application.configuration.BigDecimalSerializer
import fr.sacane.jmanager.application.configuration.LocalDateSerializer
import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import kotlinx.serialization.Serializable
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.math.BigDecimal
import java.time.LocalDate
import java.time.Month
import java.util.logging.Logger

@RestController
@RequestMapping("api/transaction")
@Adapter(Side.APPLICATION)
class TransactionController(
    private val bookTransactionUseCase: BookTransactionUseCase,
    private val deleteTransactionsByIdsUseCase: DeleteTransactionsByIdsUseCase,
    private val editTransactionUseCase: EditTransactionUseCase,
    private val findTransactionByIdUseCase: FindTransactionByIdUseCase,
    private val confirmPreviewTransactionUseCase: ConfirmPreviewTransactionUseCase,
    private val getAllRegularTransactionsUseCase: GetAllRegularTransactionsUseCase,
    private val bookRegularTransactionUseCase: BookRegularTransactionUseCase,
    private val getRegularTransactionByIdUseCase: GetRegularTransactionByIdUseCase,
    private val updateRegularTransactionUseCase: UpdateRegularTransactionUseCase,
    private val deleteRegularTransactionUseCase: DeleteRegularTransactionUseCase,
    private val deleteRegularTransactionsUseCase: DeleteRegularTransactionsUseCase,
    private val linkRegularTransactionToBookletUseCase: LinkRegularTransactionToBookletUseCase,
    private val unlinkRegularTransactionFromBookletUseCase: UnlinkRegularTransactionFromBookletUseCase,
    private val loadTransactionsForBookletForAMonthUseCase: LoadTransactionsForBookletForAMonthUseCase
) {

    companion object {
        private val logger = Logger.getLogger(TransactionController::class.java.name)
    }


    @PostMapping(consumes = [MediaType.APPLICATION_JSON_VALUE])
    fun createTransaction(
        @Valid @RequestBody userBookletResponse: UserBookletResponse
    ): ResponseEntity<TransactionResponse> {
        return bookTransactionUseCase.handle(
            BookTransactionCommand(
                token = SessionToken(currentUser.token),
                bookletLabel = userBookletResponse.bookletLabel,
                transaction = userBookletResponse.transactionResult.toModel()
            )
        ).map {
            it.toDTO()
        }.toHttpResponse()
    }

    @DeleteMapping(consumes = [MediaType.APPLICATION_JSON_VALUE])
    fun deleteByIds(
        @Valid @RequestBody transactionIds: BookletTransactionsIdRequest
    ): ResponseEntity<TransactionDeletionResponse> =
        deleteTransactionsByIdsUseCase
            .handle(DeleteTransactionsByIdsCommand(SessionToken(currentUser.token), transactionIds.bookletId.toUUID(), transactionIds.transactionIds.toUUIDs()))
            .map { it.toDTO() }
            .toHttpResponse()


    @GetMapping
    fun getTransactionsByMonthAndYearAndBookletId(
        @RequestParam("month", required = false) month: Month?,
        @RequestParam("year") year: Int,
        @RequestParam("bookletId") bookletId: String,
        @RequestParam("startDate", required = false) startDate: LocalDate?,
        @RequestParam("endDate", required = false) endDate: LocalDate?,
        @RequestParam(required = false, defaultValue = "0") page: Int,
        @RequestParam(required = false, defaultValue = "10") size: Int,
        ): ResponseEntity<TransactionListResponse> {
        validateDateRange(startDate, endDate)
        logger.info("Request transactions from booklet $bookletId for month $month and year $year")
        val response = loadTransactionsForBookletForAMonthUseCase.handle(
            LoadTransactionsForBookletForAMonthQuery(
                token = SessionToken(currentUser.token),
                bookletId = java.util.UUID.fromString(bookletId),
                month = month ?: Month.JANUARY,
                year = year,
                startDate = startDate,
                endDate = endDate,
                pageNumber = page,
                pageSize = size,
            )
        )

        return response.map {
            TransactionListResponse(
                transactions = (it.currentTransactions + it.previsionalTransactions).map { transaction -> transaction.toDTO() },
                amount = it.realSold.value.toString(),
                previewAmount = it.previsionalSold.value.toString(),
                pageNumber = it.pageNumber,
                pageSize = it.pageSize,
                totalElements = it.totalElements,
                totalPages = it.totalPages,
            )
        }.toHttpResponse().also { logger.info("Transactions fetched successfully") }
    }

    private fun validateDateRange(startDate: LocalDate?, endDate: LocalDate?) {
        if ((startDate == null) != (endDate == null)) {
            throw InvalidRequestException(
                ResultState.BAD_REQUEST.code,
                "startDate and endDate must both be provided"
            )
        }

        if (startDate != null && endDate != null && startDate.isAfter(endDate)) {
            throw InvalidRequestException(
                ResultState.BAD_REQUEST.code,
                "startDate cannot be after endDate"
            )
        }
    }

    @PatchMapping(consumes = [MediaType.APPLICATION_JSON_VALUE])
    fun patchTransaction(
        @Valid @RequestBody dto: UserBookletIdsTransactionRequest
    ): ResponseEntity<TransactionResponse> {
        logger.info("Start editing transaction => ${dto.transaction}")
        return editTransactionUseCase.handle(EditTransactionCommand(SessionToken(currentUser.token), java.util.UUID.fromString(dto.bookletId), dto.transaction.toModel()))
            .map {
                it.toDTO()
            }.toHttpResponse()
            .also { logger.info("Transaction edited successfully : ${dto.transaction}") }
    }


    @GetMapping("{id}")
    fun findById(
        @PathVariable("id") transactionID: String
    ): ResponseEntity<TransactionResult>
        = findTransactionByIdUseCase.handle(FindTransactionByIdQuery(SessionToken(currentUser.token), java.util.UUID.fromString(transactionID)))
            .map {
                it.toDTO()
            }.toHttpResponse()

    @PatchMapping("/confirm", consumes = [MediaType.APPLICATION_JSON_VALUE])
    fun confirmPreviewTransaction(
        @Valid @RequestBody command: ConfirmPreviewRequest
    ): ResponseEntity<TransactionResponse> {
        logger.info("Confirming preview Transaction...")
        return confirmPreviewTransactionUseCase.handle(
            ConfirmPreviewTransactionCommand(
                token = SessionToken(currentUser.token),
                bookletID = java.util.UUID.fromString(command.bookletID),
                transactionId = java.util.UUID.fromString(command.transactionID),
                newAmount = command.newAmount?.toAmount(),
                newDate = command.newDate
            )
        ).map {
            it.toDTO()
        }.toHttpResponse().also {
            logger.info("Preview Transaction confirmed successfully")
        }
    }


    @GetMapping("/regular")
    fun getAllRegularTransactions(
        @RequestParam(required = false, defaultValue = "0") page: Int,
        @RequestParam(required = false, defaultValue = "10") size: Int,
    ): ResponseEntity<Page<RegularTransactionDTO>> {
        return getAllRegularTransactionsUseCase.handle(GetAllRegularTransactionsQuery(SessionToken(currentUser.token), page, size))
            .map { p -> Page(p.content.map { it.toDTO() }, p.pageNumber, p.pageSize, p.totalElements, p.totalPages) }
            .toHttpResponse()
    }

    @PostMapping("/monthly", consumes = [MediaType.APPLICATION_JSON_VALUE])
    fun createMonthlyTransaction(
        @Valid @RequestBody request: MonthlyRegularTransactionRequest
    ): ResponseEntity<RegularTransactionDTO> {
        logger.info("Creating monthly transaction $request from userID ${currentUser.id}")
        if (request.bookletIds.isEmpty()) {
            throw InvalidRequestException(ResultState.BAD_REQUEST.code, "At least one booklet must be selected")
        }

        return bookRegularTransactionUseCase.handle(
            BookRegularTransactionCommand(
                SessionToken(currentUser.token),
                RegularTransaction(
                    id = RegularTransactionId(""),
                    label = request.label,
                    amount = request.value.toAmount(),
                    isIncome = request.isIncome,
                    tag = request.tagDTO.toDomain(),
                    frequencyProperty = request.frequencyProperty.frequencyToDomain(),
                    startDate = request.startDate,
                    recurrenceRule = RecurrenceRule.Monthly(request.repeatDay ?: request.startDate.dayOfMonth)
                ),
                request.bookletIds.map { java.util.UUID.fromString(it) }
            )
        ).map {
            it.toDTO()
        }.toHttpResponse().also {
            logger.info("Monthly transaction created successfully")
        }
    }

    @GetMapping("/regular/{id}")
    fun getRegularTransactionById(@PathVariable id: String): ResponseEntity<RegularTransactionDTO> {
        logger.info("Fetching regular transaction with ID $id")
        return getRegularTransactionByIdUseCase.handle(GetRegularTransactionByIdQuery(SessionToken(currentUser.token), id)).map {
            it.toDTO()
        }.toHttpResponse().also {
            logger.info("Regular transaction fetched successfully")
        }
    }

    @PatchMapping("/regular", consumes = [MediaType.APPLICATION_JSON_VALUE])
    fun updateRegularTransaction(
        @Valid @RequestBody request: UpdateRegularTransactionRequest
    ): ResponseEntity<RegularTransactionDTO> {
        logger.info("Updating regular transaction ${request.id}")
        return updateRegularTransactionUseCase.handle(
            UpdateRegularTransactionCommand(
                SessionToken(currentUser.token),
                RegularTransaction(
                    id = RegularTransactionId(request.id),
                    label = request.label,
                    amount = request.value.toAmount(),
                    isIncome = request.isIncome,
                    tag = request.tagDTO.toDomain(),
                    frequencyProperty = request.frequencyProperty.frequencyToDomain(),
                    startDate = request.startDate,
                    recurrenceRule = request.recurrenceRule.toDomain(request.startDate.dayOfMonth)
                ),
                request.bookletIds.map { java.util.UUID.fromString(it) }
            )
        ).map {
            it.toDTO()
        }.toHttpResponse().also {
            logger.info("Regular transaction updated successfully")
        }
    }

    @DeleteMapping("/regular/{id}")
    fun deleteRegularTransaction(@PathVariable id: String): ResponseEntity<Unit> {
        logger.info("Deleting regular transaction $id")
        return deleteRegularTransactionUseCase.handle(DeleteRegularTransactionCommand(SessionToken(currentUser.token), id))
            .map { }
            .toHttpResponse()
            .also {
                logger.info("Regular transaction deleted successfully")
            }
    }

    @DeleteMapping("/regular", consumes = [MediaType.APPLICATION_JSON_VALUE])
    fun deleteRegularTransactions(@Valid @RequestBody request: RegularTransactionsDeletionRequest): ResponseEntity<RegularTransactionsDeletionResponse> {
        logger.info("Bulk deleting ${request.transactionIds.size} regular transaction(s)")
        return deleteRegularTransactionsUseCase.handle(DeleteRegularTransactionsCommand(SessionToken(currentUser.token), request.transactionIds))
            .map { deletedIds ->
                RegularTransactionsDeletionResponse(deletedIds)
            }
            .toHttpResponse()
            .also {
                logger.info("Bulk regular transactions deletion finished")
            }
    }

    @PostMapping("/regular/{transactionId}/link/{bookletId}")
    fun linkRegularTransactionToBooklet(
        @PathVariable transactionId: String,
        @PathVariable bookletId: String
    ): ResponseEntity<RegularTransactionDTO> {
        logger.info("Linking booklet $bookletId to regular transaction $transactionId")
        return linkRegularTransactionToBookletUseCase.handle(
            LinkRegularTransactionToBookletCommand(
                SessionToken(currentUser.token),
                transactionId,
                java.util.UUID.fromString(bookletId)
            )
        ).map { it.toDTO() }
            .toHttpResponse()
            .also { logger.info("Booklet linked successfully") }
    }

    @DeleteMapping("/regular/{transactionId}/link/{bookletId}")
    fun unlinkRegularTransactionFromBooklet(
        @PathVariable transactionId: String,
        @PathVariable bookletId: String
    ): ResponseEntity<RegularTransactionDTO> {
        logger.info("Unlinking booklet $bookletId from regular transaction $transactionId")
        return unlinkRegularTransactionFromBookletUseCase.handle(
            UnlinkRegularTransactionFromBookletCommand(
                SessionToken(currentUser.token),
                transactionId,
                java.util.UUID.fromString(bookletId)
            )
        ).map { it.toDTO() }
            .toHttpResponse()
            .also { logger.info("Booklet unlinked successfully") }
    }
}

fun TransactionResumeResult.toDTO(): TransactionResponse {
    return TransactionResponse(
        this.transaction.id.toString(),
        this.transaction.label,
        this.transaction.date,
        this.transaction.amount.value.toString(),
        this.transaction.isIncome,
        this.transaction.tag!!.toDTO(),
        this.bookletAmount.value.toString(),
        this.transaction.isPreview
    )
}

@Serializable
data class ConfirmPreviewRequest(
    @field:NotBlank
    val bookletID: String,
    @field:NotBlank
    val transactionID: String,
    @Serializable(with = BigDecimalSerializer::class)
    val newAmount: BigDecimal?,
    @Serializable(with = LocalDateSerializer::class)
    val newDate: LocalDate?
)

@Serializable
data class TransactionDeletionResponse(
    val deletedIds: List<String>,
    val amount: String,
)

private fun TransactionDeletionResult.toDTO(): TransactionDeletionResponse = TransactionDeletionResponse(
    deletedIds = deletedIds.map { it.toString() },
    amount = bookletAmount.value.toString(),
)
