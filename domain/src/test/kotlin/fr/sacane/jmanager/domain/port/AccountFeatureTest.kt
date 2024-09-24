package fr.sacane.jmanager.domain.port

import fr.sacane.jmanager.domain.AuthenticationTest
import fr.sacane.jmanager.domain.State
import fr.sacane.jmanager.domain.fake.FakeFactory
import fr.sacane.jmanager.domain.models.*
import fr.sacane.jmanager.domain.port.api.AccountFeature
import fr.sacane.jmanager.domain.port.spi.UserRepository
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.util.*

class AccountFeatureTest {

    companion object{
        private lateinit var userRepository: UserRepository
        private lateinit var accountFeature: AccountFeature
        private lateinit var user: User
        private val tokenValue = UUID.randomUUID()
        private val session: AccessToken = AccessToken(tokenValue)
        private lateinit var element: Account
        private val accountState: State<AccountByOwner> = FakeFactory.accountState()
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
        accountState.init(listOf(
            AccountByOwner(listOf(element), user.id)
        ))
        val response = accountFeature.findAccountById(user.id, 50L, session.tokenValue)
        assertTrue(response.isSuccess())
        response.onSuccess{
            assertTrue(it.label == "test")
        }
    }

    @Test
    fun `Given an existing account it could be edit`() {
        accountState.init(listOf(
            AccountByOwner(listOf(element), user.id)
        ))
        val response = accountFeature.editAccount(userID = user.id.id!!, account = Account(element.id, Amount(BigDecimal(102)), labelAccount = element.label, initialSold = element.initialSold), session.tokenValue)
        assertTrue(response.isSuccess())
        response.onSuccess{
            val expectedAnswer = Amount(BigDecimal(102))
            val actualAmount = it.sold
            assertEquals(expectedAnswer, actualAmount)
        }
    }
}