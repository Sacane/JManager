package fr.sacane.jmanager.domain.port

import fr.sacane.jmanager.domain.AuthenticationTest
import fr.sacane.jmanager.domain.fake.FakeFactory
import fr.sacane.jmanager.domain.models.*
import fr.sacane.jmanager.domain.port.api.AccountFeature
import fr.sacane.jmanager.domain.port.spi.UserRepository
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.util.*

class AccountFeatureTest {

    companion object{
        private lateinit var userRepository: UserRepository
        private lateinit var accountFeature: AccountFeature
        private lateinit var user: User
        private val tokenValue = UUID.randomUUID()
        private val session: AccessToken = AccessToken(tokenValue)
        private lateinit var element: Account

        @JvmStatic
        @BeforeAll
        fun setup() {
            userRepository = FakeFactory.fakeUserRepository()
            user = userRepository.register("jojo", "jojo.gmail.com", Password("test")) as User
            connectUser(user)
            element = Account(50L, Amount.fromString("100 €"), "test", owner = user)
            accountFeature = FakeFactory.accountFeature
        }

        private fun connectUser(user: User) {
            FakeFactory.sessionManager.addSession(user.id, session)
        }
    }

    @AfterEach
    fun clear(){
        FakeFactory.clearAll()
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
        val accountState = FakeFactory.accountState()
        accountState.init(listOf(
            AccountByOwner(listOf(element), user.id)
        ))
        val response = accountFeature.findAccountById(user.id, 50L, session.tokenValue)
        println(response.status)
        assertTrue(response.isSuccess())
        response.onSuccess{
            assertTrue(it.label == "test")
        }
    }
}