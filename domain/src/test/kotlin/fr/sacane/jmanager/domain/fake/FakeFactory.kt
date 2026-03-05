package fr.sacane.jmanager.domain.fake

import fr.sacane.jmanager.domain.BiState
import fr.sacane.jmanager.domain.InMemoryDatabase
import fr.sacane.jmanager.domain.State
import fr.sacane.jmanager.domain.models.AccessToken
import fr.sacane.jmanager.domain.models.Role
import fr.sacane.jmanager.domain.models.Tag
import fr.sacane.jmanager.domain.models.UserId
import fr.sacane.jmanager.domain.models.transaction.regular.RegularTransaction
import fr.sacane.jmanager.domain.port.api.*
import fr.sacane.jmanager.domain.port.spi.*
import fr.sacane.jmanager.domain.port.spi.repository.BookletBalanceQueryRepository
import fr.sacane.jmanager.domain.port.spi.repository.RegularTransactionTrackerRepository
import fr.sacane.jmanager.domain.port.spi.repository.TransactionQueryRepository
import fr.sacane.jmanager.domain.port.spi.repository.UnitOfWorkTransactionProvider
import fr.sacane.jmanager.domain.usecase.CategoryDistributionCalculatorImpl
import fr.sacane.jmanager.domain.usecase.MonthlyStatsCalculatorImpl
import fr.sacane.jmanager.domain.usecase.PrevisionalTransactionFilterImpl
import fr.sacane.jmanager.domain.usecase.RegularTransactionGenerator
import fr.sacane.jmanager.domain.usecase.RegularTransactionGeneratorService
import fr.sacane.jmanager.domain.usecase.TrendCalculatorImpl
import java.util.*

object FakeFactory {
    private val inMemoryDatabase = InMemoryDatabase()
    private val  inMemoryTrackerRepository: RegularTransactionTrackerRepository = InMemoryRegularTrackerRepository(inMemoryDatabase)
    private val fakeAccountRepository: InMemoryBookletRepository = InMemoryBookletRepository(inMemoryDatabase)
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
            val booklet = inMemoryDatabase.findAccountById(bookletId) ?: return null
            return BookletBalanceQueryRepository.PersistedBalances(
                label = booklet.label,
                amount = booklet.amount.value,
            )
        }
    }

    val accountFeature = BookletFeatureImpl(
        userRepository,
        sessionManager,
        fakeAccountRepository,
        inMemoryRegularTransactionRepository,
        inMemoryRegularTransactionGenerator,
        manager,
        inMemoryTrackerRepository,
        transactionQueryRepository,
        bookletBalanceQueryRepository
    )
    val transactionFeature = TransactionFeatureImpl(transactionRepository, sessionManager, fakeAccountRepository, manager, inMemoryTagRepository, inMemoryTrackerRepository)
    val sessionFeature = UserFeatureImpl(userRepository, sessionManager, DefaultHasher, tokenGenerator)
    private val tagFeature = TagFeatureImpl(inMemoryTagRepository, sessionManager)
    val regularTransactionFeature = RegularTransactionFeatureImpl(
        inMemoryRegularTransactionRepository,
        inMemoryTagRepository,
        sessionManager,
        manager
    )
    val fileImportExportFeature = FileImportExportFeatureImpl(
        csvFileReader,
        transactionRepository,
        fakeAccountRepository,
        inMemoryTagRepository,
        sessionManager,
        manager
    )

    val statsFeature: StatsFeature by lazy {
        StatsFeatureImpl(
            session = sessionManager(),
            userRepository = fakeUserRepository(),
            bookletRepository = fakeAccountRepository,
            monthlyStatsCalculator = MonthlyStatsCalculatorImpl(),
            categoryDistributionCalculator = CategoryDistributionCalculatorImpl(inMemoryTagRepository),
            trendCalculator = TrendCalculatorImpl(),
            previsionalTransactionFilter = PrevisionalTransactionFilterImpl()
        )
    }

    fun accountState(): State<AccountByOwner>{
        return fakeAccountRepository
    }

    fun sessionState(): InMemorySessionManager = sessionManager

    fun clearAll() {
        fakeAccountRepository.clear()
        userRepository.clear()
        transactionRepository.clear()
        inMemoryTagRepository.clear()
    }

    fun fakeUserRepository(): InMemoryUserRepository {
        return userRepository
    }

    fun fakeTransactionRepository(): State<IdUserAccountByTransaction> {
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

    fun tagFeature(): TagFeature {
        return tagFeature
    }
}