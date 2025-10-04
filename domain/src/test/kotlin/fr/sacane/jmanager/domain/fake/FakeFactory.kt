package fr.sacane.jmanager.domain.fake

import fr.sacane.jmanager.domain.BiState
import fr.sacane.jmanager.domain.InMemoryDatabase
import fr.sacane.jmanager.domain.State
import fr.sacane.jmanager.domain.models.AccessToken
import fr.sacane.jmanager.domain.models.Role
import fr.sacane.jmanager.domain.models.Tag
import fr.sacane.jmanager.domain.models.UserId
import fr.sacane.jmanager.domain.port.api.*
import fr.sacane.jmanager.domain.port.spi.*
import java.util.*

object FakeFactory {
    private val inMemoryDatabase = InMemoryDatabase()
    private val fakeAccountRepository: InMemoryAccountRepository = InMemoryAccountRepository(inMemoryDatabase)
    private val transactionRepository: InMemoryTransactionRepository = InMemoryTransactionRepository(inMemoryDatabase)
    private val userRepository: InMemoryUserRepository = InMemoryUserRepository(inMemoryDatabase)
    private val inMemoryRegularTransactionRepository: InMemoryRegularTransactionRepository = InMemoryRegularTransactionRepository()
    private val manager: UnitOfWorkTransactionProviderPort = UnitOfWorkTransactionProviderPort.DEFAULT
    private val inMemoryRegularChecker: InMemoryRegularChecker = InMemoryRegularChecker()

    val tokenGenerator: TokenGenerator = object : TokenGenerator {
        override fun generateToken(userId: UserId, username: String, role: Role): AccessToken {
            return AccessToken(userId, username, "${userId.value}||${UUID.randomUUID()}||${role.name}||$username", role = role)
        }

        override fun readToken(token: String): AccessToken? {
            val parts = token.split("||")
            if (parts.size != 4) return null
            val userId = UserId(parts[0].toLong())
            val role = Role.valueOf(parts[2])
            val username = parts[3]
            return AccessToken(userId, username, token, role = role)
        }
    }

    private val sessionManager = InMemorySessionManager(tokenGenerator)

    val accountFeature = BookletFeatureImpl(userRepository, sessionManager, fakeAccountRepository, inMemoryRegularTransactionRepository)
    val transactionFeature = TransactionFeatureImpl(transactionRepository, sessionManager, fakeAccountRepository, manager, inMemoryRegularChecker)
    val sessionFeature = UserFeatureImpl(userRepository, sessionManager, DefaultHasher, tokenGenerator)
    private val inMemoryTagRepository = InMemoryTagRepository(inMemoryDatabase)
    private val tagFeature = TagFeatureImpl(inMemoryTagRepository, sessionManager)
    fun accountState(): State<AccountByOwner>{
        return fakeAccountRepository
    }

    fun sessionState(): InMemorySessionManager = sessionManager

    fun clearAll() {
        fakeAccountRepository.clear()
        userRepository.clear()
        transactionRepository.clear()
    }

    fun fakeUserRepository(): InMemoryUserRepository {
        return userRepository
    }

    fun fakeTransactionRepository(): State<IdUserAccountByTransaction> {
        return transactionRepository
    }

    fun sessionManager(): SessionManager {
        return sessionManager
    }
    fun fakeTagRepository(): BiState<UserTag, List<Tag>> {
        return inMemoryTagRepository
    }
    fun tagFeature(): TagFeature {
        return tagFeature
    }
}