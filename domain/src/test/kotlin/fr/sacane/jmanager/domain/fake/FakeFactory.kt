package fr.sacane.jmanager.domain.fake

import fr.sacane.jmanager.domain.State
import fr.sacane.jmanager.domain.port.*
import fr.sacane.jmanager.domain.port.api.*

object FakeFactory {
    private val inMemoryDatabase = InMemoryDatabase()
    private val fakeAccountRepository: InMemoryAccountRepository = InMemoryAccountRepository(inMemoryDatabase)
    private val transactionRepository: InMemoryTransactionRepository = InMemoryTransactionRepository(inMemoryDatabase)
    private val userRepository: InMemoryUserRepository = InMemoryUserRepository(inMemoryDatabase)
    val sessionManager: SessionManager = InMemorySessionManager()
    val accountFeature = AccountFeatureImpl(userRepository, sessionManager, fakeAccountRepository)
    val transactionFeature = TransactionFeatureImpl(transactionRepository, userRepository, sessionManager, fakeAccountRepository)
    val previewTransactionFeature = PreviewTransactionFeatureImpl(fakeAccountRepository, transactionRepository, sessionManager)
    fun accountState(): State<AccountByOwner>{
        return fakeAccountRepository
    }

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