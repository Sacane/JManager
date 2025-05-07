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

object FakeFactory {
    private val inMemoryDatabase = InMemoryDatabase()
    private val fakeAccountRepository: InMemoryAccountRepository = InMemoryAccountRepository(inMemoryDatabase)
    private val transactionRepository: InMemoryTransactionRepository = InMemoryTransactionRepository(inMemoryDatabase)
    private val userRepository: InMemoryUserRepository = InMemoryUserRepository(inMemoryDatabase)
    private val manager: InfraTransactionProviderPort = InfraTransactionProviderPort.DEFAULT
    private val sessionManager = InMemorySessionManager()

    private val tokenGenerator: TokenGenerator = object : TokenGenerator {
        override fun generateToken(userId: UserId, role: Role): AccessToken {
            return AccessToken(userId, "token", role = role)
        }

        override fun readToken(token: String): AccessToken? {
            TODO("Not yet implemented")
        }
    }

    val accountFeature = AccountFeatureImpl(userRepository, sessionManager, fakeAccountRepository)
    val transactionFeature = TransactionFeatureImpl(transactionRepository, sessionManager, fakeAccountRepository, manager)
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