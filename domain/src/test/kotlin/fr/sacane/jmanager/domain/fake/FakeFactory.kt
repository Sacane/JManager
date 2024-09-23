package fr.sacane.jmanager.domain.fake

import fr.sacane.jmanager.domain.State
import fr.sacane.jmanager.domain.models.UserId
import fr.sacane.jmanager.domain.port.AccountByOwner
import fr.sacane.jmanager.domain.port.InMemoryAccountRepository
import fr.sacane.jmanager.domain.port.InMemoryUserProvider
import fr.sacane.jmanager.domain.port.InMemoryUserRepository
import fr.sacane.jmanager.domain.port.api.AccountFeatureImpl
import fr.sacane.jmanager.domain.port.api.InMemorySessionManager
import fr.sacane.jmanager.domain.port.api.SessionManager
import fr.sacane.jmanager.domain.port.spi.UserRepository
import java.util.UUID

object FakeFactory {
    private val userProvider = InMemoryUserProvider()
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