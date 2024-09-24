package fr.sacane.jmanager.domain.fake

import fr.sacane.jmanager.domain.State
import fr.sacane.jmanager.domain.port.AccountByOwner
import fr.sacane.jmanager.domain.port.InMemoryAccountRepository
import fr.sacane.jmanager.domain.port.InMemoryDatabase
import fr.sacane.jmanager.domain.port.InMemoryUserRepository
import fr.sacane.jmanager.domain.port.api.AccountFeatureImpl
import fr.sacane.jmanager.domain.port.api.InMemorySessionManager
import fr.sacane.jmanager.domain.port.api.SessionManager
import fr.sacane.jmanager.domain.port.spi.UserRepository

object FakeFactory {
    private val userProvider = InMemoryDatabase()
    private val fakeAccountRepository: InMemoryAccountRepository = InMemoryAccountRepository(userProvider)
    private val userRepository = InMemoryUserRepository(userProvider)
    val sessionManager: SessionManager = InMemorySessionManager()
    val accountFeature = AccountFeatureImpl(userRepository, sessionManager, fakeAccountRepository)


    fun accountState(): State<AccountByOwner>{
        return fakeAccountRepository
    }

    fun clearAll() {
        fakeAccountRepository.clear()
    }

    fun fakeUserRepository(): UserRepository {
        return userRepository
    }


}