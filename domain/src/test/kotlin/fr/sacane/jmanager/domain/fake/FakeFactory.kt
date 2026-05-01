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

    val findBookletByIdService = FindBookletByIdService(bookletRepository)
    val editBookletService = EditBookletService(bookletRepository)
    val deleteBookletByIdService = DeleteBookletByIdService(bookletRepository, manager, inMemoryTrackerRepository)
    val findByLabelAndUserIdService = FindByLabelAndUserIdService(userRepository)
    val findAllRegisteredBookletsService = FindAllRegisteredBookletsService(userRepository)
    val saveBookletService = SaveBookletService(userRepository, bookletRepository)
    val loadTransactionsForBookletForAMonthService = LoadTransactionsForBookletForAMonthService(
        bookletRepository, inMemoryRegularTransactionRepository, inMemoryRegularTransactionGenerator,
        manager, inMemoryTrackerRepository, transactionQueryRepository, paginator
    )
    val loadBalancesForBookletForAMonthService = LoadBalancesForBookletForAMonthService(
        inMemoryRegularTransactionRepository, manager, transactionQueryRepository,
        bookletBalanceQueryRepository, inMemoryRegularTransactionGenerator
    )
    val regenerateDeletedPrevisionalTransactionsService = RegenerateDeletedPrevisionalTransactionsService(
        bookletRepository, inMemoryRegularTransactionRepository, inMemoryRegularTransactionGenerator,
        manager, inMemoryTrackerRepository, transactionQueryRepository
    )
    val bookTransactionService = BookTransactionService(transactionRepository, bookletRepository, manager, inMemoryTagRepository)
    val retrieveTransactionsByMonthAndYearService = RetrieveTransactionsByMonthAndYearService(transactionRepository)
    val editTransactionService = EditTransactionService(transactionRepository, bookletRepository, manager)
    val findTransactionByIdService = FindTransactionByIdService(transactionRepository)
    val deleteTransactionsByIdsService = DeleteTransactionsByIdsService(transactionRepository, bookletRepository, manager, inMemoryTrackerRepository)
    val excludeVirtualTransactionService = ExcludeVirtualTransactionService(bookletRepository, inMemoryTrackerRepository)
    val confirmPreviewTransactionService = ConfirmPreviewTransactionService(transactionRepository, bookletRepository, manager)
    val confirmVirtualTransactionService = ConfirmVirtualTransactionService(transactionRepository, bookletRepository, inMemoryTrackerRepository, manager)
    val loginService = LoginService(userRepository, sessionManager, DefaultHasher, tokenGenerator)
    val logoutService = LogoutService(sessionManager)
    val refreshSessionService = RefreshSessionService(sessionManager, userRepository, tokenGenerator)
    val registerUserService = RegisterUserService(userRepository, DefaultHasher)
    val createAdminIfNotExistsService = CreateAdminIfNotExistsService(userRepository, DefaultHasher)
    val getUserSettingsService = GetUserSettingsService(userRepository)
    val updateUserSettingsService = UpdateUserSettingsService(userRepository, bookletRepository)
    private val addTagService = AddTagService(inMemoryTagRepository)
    private val getAllTagsService = GetAllTagsService(inMemoryTagRepository)
    private val addDefaultTagsService = AddDefaultTagsService(inMemoryTagRepository)
    private val deleteTagService = DeleteTagService(inMemoryTagRepository, transactionRepository, inMemoryRegularTransactionRepository)
    private val defaultTagService = DefaultTagService(inMemoryTagRepository)
    private val editTagService = EditTagService(inMemoryTagRepository)
    val getAllRegularTransactionsService = GetAllRegularTransactionsService(inMemoryRegularTransactionRepository, paginator)
    val bookRegularTransactionService = BookRegularTransactionService(inMemoryRegularTransactionRepository, manager)
    val getRegularTransactionByIdService = GetRegularTransactionByIdService(inMemoryRegularTransactionRepository)
    val updateRegularTransactionService = UpdateRegularTransactionService(inMemoryRegularTransactionRepository, manager)
    val deleteRegularTransactionService = DeleteRegularTransactionService(inMemoryRegularTransactionRepository, manager)
    val deleteRegularTransactionsService = DeleteRegularTransactionsService(inMemoryRegularTransactionRepository, manager)
    val linkRegularTransactionToBookletService = LinkRegularTransactionToBookletService(inMemoryRegularTransactionRepository, manager)
    val unlinkRegularTransactionFromBookletService = UnlinkRegularTransactionFromBookletService(inMemoryRegularTransactionRepository, manager, inMemoryTrackerRepository)
    val validateCsvFileService = ValidateCsvFileService(csvFileReader, bookletRepository, inMemoryTagRepository)
    val importTransactionsFromCsvService = ImportTransactionsFromCsvService(csvFileReader, transactionRepository, bookletRepository, inMemoryTagRepository, manager)
    val exportTransactionsToCsvService = ExportTransactionsToCsvService()

    val getMonthlyBookletStatsService = GetMonthlyBookletStatsService(bookletRepository, MonthlyStatsCalculatorImpl())
    val getCategoryDistributionService = GetCategoryDistributionService(bookletRepository, CategoryDistributionCalculatorImpl(inMemoryTagRepository))
    val getTrendStatsService = GetTrendStatsService(bookletRepository, TrendCalculatorImpl())
    val getPrevisionalTransactionsService = GetPrevisionalTransactionsService(bookletRepository, PrevisionalTransactionFilterImpl())
    val getDailyTrendStatsService = GetDailyTrendStatsService(bookletRepository, DailyTrendCalculatorImpl())

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