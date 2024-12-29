package fr.sacane.jmanager.domain.fake

import fr.sacane.jmanager.domain.BiState
import fr.sacane.jmanager.domain.InMemoryDatabase
import fr.sacane.jmanager.domain.State
import fr.sacane.jmanager.domain.models.AccessToken
import fr.sacane.jmanager.domain.port.api.*
import fr.sacane.jmanager.domain.port.spi.DefaultHasher
import fr.sacane.jmanager.domain.port.spi.Hasher
import fr.sacane.jmanager.domain.port.spi.InfraTransactionProviderPort

object FakeFactory {
    private val inMemoryDatabase = InMemoryDatabase()
    private val fakeAccountRepository: InMemoryAccountRepository = InMemoryAccountRepository(inMemoryDatabase)
    private val transactionRepository: InMemoryTransactionRepository = InMemoryTransactionRepository(inMemoryDatabase)
    private val userRepository: InMemoryUserRepository = InMemoryUserRepository(inMemoryDatabase)
    private val manager: InfraTransactionProviderPort = InfraTransactionProviderPort.DEFAULT
    private val sessionManager: SessionFakeState = SessionFakeState()
    val accountFeature = AccountFeatureImpl(userRepository, sessionManager, fakeAccountRepository)
    val transactionFeature = TransactionFeatureImpl(transactionRepository, sessionManager, fakeAccountRepository, manager)
    val sessionFeature = SessionFeatureImpl(userRepository, sessionManager, DefaultHasher)
    fun accountState(): State<AccountByOwner>{
        return fakeAccountRepository
    }

    fun sessionState(): BiState<List<UserSessionEntry>, List<AccessToken>> = sessionManager

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
}