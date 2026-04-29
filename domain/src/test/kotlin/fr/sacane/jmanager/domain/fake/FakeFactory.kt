package fr.sacane.jmanager.domain.fake

import fr.sacane.jmanager.domain.BiState
import fr.sacane.jmanager.domain.InMemoryDatabase
import fr.sacane.jmanager.domain.PaginatorImpl
import fr.sacane.jmanager.domain.State
import fr.sacane.jmanager.domain.models.AccessToken
import fr.sacane.jmanager.domain.models.Role
import fr.sacane.jmanager.domain.models.Tag
import fr.sacane.jmanager.domain.models.UserId
import fr.sacane.jmanager.domain.models.transaction.regular.RegularTransaction
import fr.sacane.jmanager.domain.port.input.booklet.*
import fr.sacane.jmanager.domain.port.input.csv.*
import fr.sacane.jmanager.domain.port.input.regularTransaction.*
import fr.sacane.jmanager.domain.port.input.stats.*
import fr.sacane.jmanager.domain.port.input.tag.*
import fr.sacane.jmanager.domain.port.input.transaction.*
import fr.sacane.jmanager.domain.port.input.user.*
import fr.sacane.jmanager.domain.port.output.*
import fr.sacane.jmanager.domain.port.output.repository.BookletBalanceQueryRepository
import fr.sacane.jmanager.domain.port.output.repository.RegularTransactionTrackerRepository
import fr.sacane.jmanager.domain.port.output.repository.TransactionQueryRepository
import fr.sacane.jmanager.domain.port.output.repository.UnitOfWorkTransactionProvider
import fr.sacane.jmanager.domain.usecase.CategoryDistributionCalculatorImpl
import fr.sacane.jmanager.domain.usecase.DailyTrendCalculatorImpl
import fr.sacane.jmanager.domain.usecase.MonthlyStatsCalculatorImpl
import fr.sacane.jmanager.domain.usecase.PrevisionalTransactionFilterImpl
import fr.sacane.jmanager.domain.usecase.RegularTransactionGenerator
import fr.sacane.jmanager.domain.usecase.RegularTransactionGeneratorService
import fr.sacane.jmanager.domain.usecase.TrendCalculatorImpl
import java.util.*

object FakeFactory {
    private val inMemoryDatabase = InMemoryDatabase()
    private val  inMemoryTrackerRepository: RegularTransactionTrackerRepository = InMemoryRegularTrackerRepository(inMemoryDatabase)
    private val  bookletRepository: InMemoryBookletRepository = InMemoryBookletRepository(inMemoryDatabase)
    private val transactionRepository: InMemoryTransactionRepository = InMemoryTransactionRepository(inMemoryDatabase)
    private val transactionQueryRepository: TransactionQueryRepository = InMemoryTransactionQueryRepository(inMemoryDatabase)
    private val userRepository: InMemoryUserRepository = InMemoryUserRepository(inMemoryDatabase)
    private val inMemoryRegularTransactionRepository: InMemoryRegularTransactionRepository = InMemoryRegularTransactionRepository(inMemoryDatabase)
    private val manager: UnitOfWorkTransactionProvider = UnitOfWorkTransactionProvider.DEFAULT
    private val inMemoryRegularTransactionGenerator: RegularTransactionGenerator = RegularTransactionGeneratorService(
        transactionRepository, inMemoryTrackerRepository
    )
    val regularTransactionState: BiState<List<UserRegularTransaction>, List<RegularTransaction>> = inMemoryRegularTransactionRepository

    val tokenGenerator: TokenGenerator = object : TokenGenerator {
        override fun generateToken(userId: UserId, username: String, roles: Set<Role>): AccessToken {
            return AccessToken(userId, username, "${userId.value}||${UUID.randomUUID()}||$username||${roles.joinToString("|") { it.name }}", roles = roles)
        }

        override fun readToken(token: String): AccessToken? {
            val parts = token.split("||")
            if (parts.size != 4) return null
            val userId = UserId(UUID.fromString(parts[0]))
            val roleStrings = parts[2].split("|").mapNotNull {
                try {
                    Role.valueOf(it)
                } catch (_: IllegalArgumentException) {
                    null
                }
            }.toSet()
            val username = parts[3]
            return AccessToken(userId, username, token, roles = roleStrings)
        }
    }

    private val sessionManager = InMemorySessionManager(tokenGenerator)
    private val inMemoryTagRepository = InMemoryTagRepository(inMemoryDatabase)
    private val csvFileReader = InMemoryCsvFileReader()

    private val bookletBalanceQueryRepository: BookletBalanceQueryRepository = object : BookletBalanceQueryRepository {
        override fun findPersistedBalances(bookletId: UUID): BookletBalanceQueryRepository.PersistedBalances? {
            val booklet = inMemoryDatabase.findBookletById(bookletId) ?: return null
            return BookletBalanceQueryRepository.PersistedBalances(
                label = booklet.label,
                amount = booklet.amount.value,
            )
        }
    }

    private val paginator = PaginatorImpl()

    val findBookletByIdService = FindBookletByIdService(sessionManager, bookletRepository)
    val editBookletService = EditBookletService(sessionManager, bookletRepository)
    val deleteBookletByIdService = DeleteBookletByIdService(sessionManager, bookletRepository, manager, inMemoryTrackerRepository)
    val findByLabelAndUserIdService = FindByLabelAndUserIdService(sessionManager, userRepository)
    val findAllRegisteredBookletsService = FindAllRegisteredBookletsService(sessionManager, userRepository)
    val saveBookletService = SaveBookletService(sessionManager, userRepository, bookletRepository)
    val loadTransactionsForBookletForAMonthService = LoadTransactionsForBookletForAMonthService(
        sessionManager, bookletRepository, inMemoryRegularTransactionRepository, inMemoryRegularTransactionGenerator,
        manager, inMemoryTrackerRepository, transactionQueryRepository, paginator
    )
    val loadBalancesForBookletForAMonthService = LoadBalancesForBookletForAMonthService(
        sessionManager, inMemoryRegularTransactionRepository, manager, transactionQueryRepository,
        bookletBalanceQueryRepository, inMemoryRegularTransactionGenerator
    )
    val regenerateDeletedPrevisionalTransactionsService = RegenerateDeletedPrevisionalTransactionsService(
        sessionManager, bookletRepository, inMemoryRegularTransactionRepository, inMemoryRegularTransactionGenerator,
        manager, inMemoryTrackerRepository, transactionQueryRepository
    )
    val bookTransactionService = BookTransactionService(transactionRepository, sessionManager, bookletRepository, manager, inMemoryTagRepository)
    val retrieveTransactionsByMonthAndYearService = RetrieveTransactionsByMonthAndYearService(transactionRepository, sessionManager)
    val editTransactionService = EditTransactionService(transactionRepository, sessionManager, bookletRepository, manager)
    val findTransactionByIdService = FindTransactionByIdService(transactionRepository, sessionManager)
    val deleteTransactionsByIdsService = DeleteTransactionsByIdsService(transactionRepository, sessionManager, bookletRepository, manager, inMemoryTrackerRepository)
    val excludeVirtualTransactionService = ExcludeVirtualTransactionService(sessionManager, bookletRepository, inMemoryTrackerRepository)
    val confirmPreviewTransactionService = ConfirmPreviewTransactionService(transactionRepository, sessionManager, bookletRepository, manager)
    val loginService = LoginService(userRepository, sessionManager, DefaultHasher, tokenGenerator)
    val logoutService = LogoutService(sessionManager)
    val refreshSessionService = RefreshSessionService(sessionManager, userRepository, tokenGenerator)
    val registerUserService = RegisterUserService(userRepository, DefaultHasher)
    val createAdminIfNotExistsService = CreateAdminIfNotExistsService(userRepository, DefaultHasher)
    val getUserSettingsService = GetUserSettingsService(sessionManager, userRepository)
    val updateUserSettingsService = UpdateUserSettingsService(sessionManager, userRepository, bookletRepository)
    private val addTagService = AddTagService(inMemoryTagRepository, sessionManager)
    private val getAllTagsService = GetAllTagsService(inMemoryTagRepository, sessionManager)
    private val addDefaultTagsService = AddDefaultTagsService(inMemoryTagRepository)
    private val deleteTagService = DeleteTagService(inMemoryTagRepository, transactionRepository, inMemoryRegularTransactionRepository, sessionManager)
    private val defaultTagService = DefaultTagService(inMemoryTagRepository, sessionManager)
    private val editTagService = EditTagService(inMemoryTagRepository, sessionManager)
    val getAllRegularTransactionsService = GetAllRegularTransactionsService(inMemoryRegularTransactionRepository, sessionManager, paginator)
    val bookRegularTransactionService = BookRegularTransactionService(inMemoryRegularTransactionRepository, sessionManager, manager)
    val getRegularTransactionByIdService = GetRegularTransactionByIdService(inMemoryRegularTransactionRepository, sessionManager)
    val updateRegularTransactionService = UpdateRegularTransactionService(inMemoryRegularTransactionRepository, sessionManager, manager)
    val deleteRegularTransactionService = DeleteRegularTransactionService(inMemoryRegularTransactionRepository, sessionManager, manager)
    val deleteRegularTransactionsService = DeleteRegularTransactionsService(inMemoryRegularTransactionRepository, sessionManager, manager)
    val linkRegularTransactionToBookletService = LinkRegularTransactionToBookletService(inMemoryRegularTransactionRepository, sessionManager, manager)
    val unlinkRegularTransactionFromBookletService = UnlinkRegularTransactionFromBookletService(inMemoryRegularTransactionRepository, sessionManager, manager, inMemoryTrackerRepository)
    val validateCsvFileService = ValidateCsvFileService(csvFileReader, bookletRepository, inMemoryTagRepository, sessionManager)
    val importTransactionsFromCsvService = ImportTransactionsFromCsvService(csvFileReader, transactionRepository, bookletRepository, inMemoryTagRepository, sessionManager, manager)
    val exportTransactionsToCsvService = ExportTransactionsToCsvService(sessionManager)

    val getMonthlyBookletStatsService = GetMonthlyBookletStatsService(sessionManager(), bookletRepository, MonthlyStatsCalculatorImpl())
    val getCategoryDistributionService = GetCategoryDistributionService(sessionManager(), bookletRepository, CategoryDistributionCalculatorImpl(inMemoryTagRepository))
    val getTrendStatsService = GetTrendStatsService(sessionManager(), bookletRepository, TrendCalculatorImpl())
    val getPrevisionalTransactionsService = GetPrevisionalTransactionsService(sessionManager(), bookletRepository, PrevisionalTransactionFilterImpl())
    val getDailyTrendStatsService = GetDailyTrendStatsService(sessionManager(), bookletRepository, DailyTrendCalculatorImpl())

    fun bookletState(): State<BookletsByOwner>{
        return bookletRepository
    }

    fun sessionState(): InMemorySessionManager = sessionManager

    fun clearAll() {
        bookletRepository.clear()
        userRepository.clear()
        transactionRepository.clear()
        inMemoryTagRepository.clear()
    }

    fun fakeUserRepository(): InMemoryUserRepository {
        return userRepository
    }

    fun fakeTransactionRepository(): State<IdBookletByTransaction> {
        return transactionRepository
    }

    fun transactionRepository(): InMemoryTransactionRepository {
        return transactionRepository
    }

    fun trackerRepository(): RegularTransactionTrackerRepository {
        return inMemoryTrackerRepository
    }

    fun regularTransactionGenerator(): RegularTransactionGenerator {
        return inMemoryRegularTransactionGenerator
    }

    fun sessionManager(): SessionManager {
        return sessionManager
    }
    fun tagTestState(): BiState<UserTag, List<Tag>> {
        return inMemoryTagRepository
    }
    fun fakeTagRepository(): InMemoryTagRepository {
        return inMemoryTagRepository
    }

    fun addTagUseCase(): AddTagUseCase = addTagService
    fun getAllTagsUseCase(): GetAllTagsUseCase = getAllTagsService
    fun addDefaultTagsUseCase(): AddDefaultTagsUseCase = addDefaultTagsService
    fun deleteTagUseCase(): DeleteTagUseCase = deleteTagService
    fun defaultTagUseCase(): DefaultTagUseCase = defaultTagService
    fun editTagUseCase(): EditTagUseCase = editTagService
}