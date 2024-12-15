package fr.sacane.jmanager.domain.port

import fr.sacane.jmanager.domain.*
import fr.sacane.jmanager.domain.utils.Result
import fr.sacane.jmanager.domain.fake.FakeFactory
import fr.sacane.jmanager.domain.models.*
import fr.sacane.jmanager.domain.port.api.AccountFeature
import fr.sacane.jmanager.domain.port.spi.UserRepository
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.assertNull
import java.math.BigDecimal
import java.util.*

class AccountFeatureTest {

    companion object{
        private val userRepository: UserRepository = FakeFactory.fakeUserRepository()
        private var accountFeature: AccountFeature = FakeFactory.accountFeature
        private val user = userRepository.register("jojo", "test") as User
        private val tokenValue = UUID.randomUUID()
        private val session: AccessToken = AccessToken(tokenValue)
        private val accountState: State<AccountByOwner> = FakeFactory.accountState()
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
        private val element = Account(Amount.fromString("100", "€".asCurrency()), "test", owner = user, id = 50L)
        override val action: List<Result<out Any>>
            get() = listOf(
                accountFeature.findAccountById(user.id, 50L, UUID.randomUUID()),
                accountFeature.save(user.id, UUID.randomUUID(), element)
            )
    }

    @Test
    fun `Should find account by its Id`() {
        connectUser(user)
        val element = Account(Amount.fromString("100", "€".asCurrency()), "test", owner = user, id=50L)
        accountState.init(listOf(
            AccountByOwner(listOf(element), user.id)
        ))
        accountFeature.findAccountById(user.id, 50L, session.tokenValue)
            .assertTrue {
                this.label == "test"
            }
    }

    @Test
    fun `Given an existing account it could be edit`() {
        connectUser(user)
        val element = Account(Amount.fromString("100", "€".asCurrency()), "test", owner = user, id = 50L)
        accountState.init(listOf(
            AccountByOwner(listOf(element), user.id)
        ))
        val account = Account(
            Amount(BigDecimal(102)),
            labelAccount = element.label,
            initialSold = element.initialSold,
            owner = user,
            id= element.id,
        )
        val response = accountFeature.editAccount(userID = user.id.value!!, account = account, session.tokenValue)

        val expectedAnswer = Amount(BigDecimal(102))

        response.map { it.amount }.assertEquals(expectedAnswer)
    }

    @Test
    fun `As an owner of an account, I can delete it`() {
        val otherUser = userRepository.register("jojo",  "test") as User
        connectUser(otherUser)
        val element = Account( Amount.fromString("100", "€".asCurrency()), "test", owner = user, id = 50L)
        accountState.init(listOf(
            AccountByOwner(listOf(element), otherUser.id)
        ))

        accountFeature.deleteAccountById(otherUser.id, element.id!!, session.tokenValue).assertTrue {
            val accounts = accountState.getStates()

            val expectedAccountSize = 0
            val actualAccountSize = accounts.find { it.userId == otherUser.id }?.account?.size ?: throw Error()
            expectedAccountSize == actualAccountSize
        }

        val accounts = accountState.getStates()
        val ofUser = accounts.find { it.userId == otherUser.id }!!

        assertNull(ofUser.existsById(50))
    }

    @Test
    fun `As an account's owner, I can retrieve it by its label`() {
        val otherUser = userRepository.register("jojo","test") as User
        connectUser(otherUser)
        val element = Account(Amount.fromString("100", "€".asCurrency()), "test", owner = user, id = 50L)
        accountState.init(listOf(
            AccountByOwner(listOf(element), otherUser.id)
        ))

        accountFeature.findByLabelAndUserId(otherUser.id, session.tokenValue, element.label)
            .assertTrue {
            this.label == "test" && this.amount == Amount(100)
        }
    }

    @Test
    fun `As an account's owner,  I can retrieve All of my Registered Accounts`() {
        val otherUser = userRepository.register("jojo", "test") as User
        connectUser(otherUser)
        val account = Account(Amount.fromString("100", "€".asCurrency()), "test1", owner = user, id = 50L)
        val account2 = Account(Amount.fromString("100", "€".asCurrency()), "test2", owner = user, id = 51L)
        val account3 = Account( Amount.fromString("100", "€".asCurrency()), "test3", owner = user, id= 52L)
        val account4 = Account( Amount.fromString("100", "€".asCurrency()), "test4", owner = user, id = 53L)
        val expectedAccount = listOf(
            account,
            account2,
            account3,
            account4
        )
        accountState.init(listOf(
            AccountByOwner(expectedAccount, otherUser.id)
        ))

        accountFeature.findAllRegisteredAccounts(otherUser.id, session.tokenValue)
            .assertEquals(expectedAccount)
    }

    @Test
    fun `As a Jmanager user, I can create new account`() {
        val otherUser = userRepository.register("jojo", "test") as User
        connectUser(otherUser)

        val accountToSave = Account( Amount.fromString("100", "€".asCurrency()), "test1", owner = otherUser, id = 50L)

        accountFeature.save(otherUser.id, session.tokenValue, accountToSave)
            .assertTrue {
                val expectedAmount = Amount(100)
                val expectedLabelAccount = "test1"
                this.amount == expectedAmount && this.label == expectedLabelAccount
            }
    }

    @Test
    fun `As an account's owner, I cannot register an existing account with the same label`() {
        val otherUser = userRepository.register("jojo","test") as User
        connectUser(otherUser)

        accountState.init(listOf(
            AccountByOwner(listOf(Account(Amount.fromString("100", "€".asCurrency()), "test1", owner = otherUser, id = 50L)), otherUser.id)
        ))

        val accountToSave = Account( Amount.fromString("150", "€".asCurrency()), "test1", owner = otherUser, id = 51L)
        accountFeature.save(otherUser.id, session.tokenValue, accountToSave)
            .assertFailure()
    }
}