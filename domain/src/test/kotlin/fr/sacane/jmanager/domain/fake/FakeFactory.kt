package fr.sacane.jmanager.domain.fake

import fr.sacane.jmanager.domain.State
import fr.sacane.jmanager.domain.port.*
import fr.sacane.jmanager.domain.port.api.AccountFeatureImpl
import fr.sacane.jmanager.domain.port.api.InMemorySessionManager
import fr.sacane.jmanager.domain.port.api.SessionManager
import fr.sacane.jmanager.domain.port.api.TransactionFeatureImpl

object FakeFactory {
    private val inMemoryDatabase = InMemoryDatabase()
    private val fakeAccountRepository: InMemoryAccountRepository = InMemoryAccountRepository(inMemoryDatabase)
    private val transactionRepository: InMemoryTransactionRepository = InMemoryTransactionRepository(inMemoryDatabase)
    private val userRepository: InMemoryUserRepository = InMemoryUserRepository(inMemoryDatabase)
    val sessionManager: SessionManager = InMemorySessionManager()
    val accountFeature = AccountFeatureImpl(userRepository, sessionManager, fakeAccountRepository)
    val transactionFeature = TransactionFeatureImpl(transactionRepository, userRepository, sessionManager, fakeAccountRepository)
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
}