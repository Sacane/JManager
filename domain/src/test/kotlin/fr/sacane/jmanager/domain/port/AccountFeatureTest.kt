package fr.sacane.jmanager.domain.port

import fr.sacane.jmanager.domain.models.*
import fr.sacane.jmanager.domain.port.api.AccountFeature
import fr.sacane.jmanager.domain.port.api.AccountFeatureImpl
import fr.sacane.jmanager.domain.port.api.SessionManager
import fr.sacane.jmanager.domain.port.spi.TransactionRegister
import fr.sacane.jmanager.domain.port.spi.UserRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.util.*

class AccountFeatureTest {

    companion object{
        private val inMemoryUserProvider = InMemoryUserProvider()
        private val register: TransactionRegister = InMemoryTransactionRepository(inMemoryUserProvider)
        private val sessionManager: SessionManager = SessionManager()
        private val userRepository: UserRepository = InMemoryUserRepository(inMemoryUserProvider)
        private val accountFeature: AccountFeature = AccountFeatureImpl(register = register, session = sessionManager, userRepository = userRepository)
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
            user.accounts.add(element)
            accountFeature.save(user.id, tokenValue!!, element)
        }

        private fun connectUser(user: User) {
            sessionManager.addSession(user.id, session)
        }
    }

    @Nested
    inner class AuthenticationTest {
        @Test
        fun `Authenticate on findAccountById method`() {
            val response = accountFeature.findAccountById(user.id, 50L, UUID.randomUUID())

            assertTrue(response.isFailure())
            assertEquals(ResponseState.UNAUTHORIZED, response.status)
        }

        @Test
        fun `Authenticate on save`() {
            val response = accountFeature.save(user.id, UUID.randomUUID(), element)
            assertTrue(response.isFailure())
            assertEquals(ResponseState.UNAUTHORIZED, response.status)
        }
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