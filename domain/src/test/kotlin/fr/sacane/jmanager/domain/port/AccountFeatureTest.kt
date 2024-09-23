package fr.sacane.jmanager.domain.port

import fr.sacane.jmanager.domain.AuthenticationTest
import fr.sacane.jmanager.domain.models.*
import fr.sacane.jmanager.domain.port.api.AccountFeature
import fr.sacane.jmanager.domain.port.api.AccountFeatureImpl
import fr.sacane.jmanager.domain.port.api.InMemorySessionManager
import fr.sacane.jmanager.domain.port.spi.UserRepository
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.util.*

class AccountFeatureTest {

    companion object{
        private val inMemoryUserProvider = InMemoryUserProvider()
        private val inMemorySessionManager: InMemorySessionManager = InMemorySessionManager()
        private val userRepository: UserRepository = InMemoryUserRepository(inMemoryUserProvider)
        private val accountRepository = InMemoryAccountRepository()
        private val accountFeature: AccountFeature = AccountFeatureImpl(session = inMemorySessionManager, userRepository = userRepository, accountRepository = accountRepository)
        private lateinit var user: User
        private val tokenValue = UUID.randomUUID()
        private val session: AccessToken = AccessToken(tokenValue)
        private lateinit var element: Account

        @JvmStatic
        @BeforeAll
        fun setup() {
            user = userRepository.register("jojo", "jojo.gmail.com", Password("test")) as User
            connectUser(user)
            element = Account(50L, Amount.fromString("100 €"), "test", owner = user)
            user.addAccount(element)
            accountFeature.save(user.id, tokenValue!!, element)
        }

        private fun connectUser(user: User) {
            inMemorySessionManager.addSession(user.id, session)
        }
    }

    @Nested
    inner class AccountFeatureAuthTest: AuthenticationTest {
        override val action: List<Response<out Any>>
            get() = listOf(
                accountFeature.findAccountById(user.id, 50L, UUID.randomUUID()),
                accountFeature.save(user.id, UUID.randomUUID(), element)
            )
    }

    @Test
    fun `Should find account by its Id`() {
        val response = accountFeature.findAccountById(user.id, 50L, session.tokenValue)
        assertTrue(response.isSuccess())
        response.onSuccess{
            assertTrue(it.label == "test")
        }
    }
}