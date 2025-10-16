package fr.sacane.jmanager.domain.port

import fr.sacane.jmanager.domain.*
import fr.sacane.jmanager.domain.fake.AccountByOwner
import fr.sacane.jmanager.domain.fake.FakeFactory
import fr.sacane.jmanager.domain.models.*
import fr.sacane.jmanager.domain.port.api.BookletFeature
import fr.sacane.jmanager.domain.port.spi.UserRepository
import fr.sacane.jmanager.domain.utils.Result
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.util.*

class AccountFeatureTest: FeatureTest() {

    companion object{
        private val userRepository: UserRepository = FakeFactory.fakeUserRepository()
        private var bookletFeature: BookletFeature = FakeFactory.accountFeature
        private val user = userRepository.register("jojo", "test") as User
        private val tokenValue = "${user.id.value}||${UUID.randomUUID()}||${Role.USER.name}||${user.username}"
        private val session: AccessToken = AccessToken(userId = user.id, user.username, tokenValue)
        private val accountState: State<AccountByOwner> = FakeFactory.accountState()
        private fun connectUser(user: User) {
            FakeFactory.sessionManager().addSession(user.id, session)
        }
    }

    @AfterEach
    fun clear(){
        FakeFactory.clearAll()
    }

    @Nested
    inner class AccountFeatureAuthTest: AuthenticationTest {
        private val element = Booklet(Amount.fromString("100", "€".asCurrency()), "test", owner = user, id = 50L)
        override val action: List<Result<out Any>>
            get() = listOf(
                bookletFeature.findAccountById(50L, UUID.randomUUID().toString()),
                bookletFeature.save(UUID.randomUUID().toString(), element)
            )
    }

    @Test
    fun `Should find account by its Id`() {
        connectUser(user)
        val element = Booklet(Amount.fromString("100", "€".asCurrency()), "test", owner = user, id=50L)
        accountState.init(listOf(
            AccountByOwner(listOf(element), user.id)
        ))
        bookletFeature.findAccountById(50L, session.tokenValue)
            .assertTrue {
                this.label == "test"
            }
    }

    @Test
    fun `Given an existing account it could be edit`() {
        connectUser(user)
        val element = Booklet(Amount.fromString("100", "€".asCurrency()), "test", owner = user, id = 50L)
        accountState.init(listOf(
            AccountByOwner(listOf(element), user.id)
        ))
        val booklet = Booklet(
            Amount(BigDecimal(102)),
            labelAccount = element.label,
            initialSold = element.initialSold,
            owner = user,
            id= element.id,
        )
        val response = bookletFeature.editAccount(booklet = booklet, session.tokenValue)

        val expectedAnswer = Amount(BigDecimal(102))

        response.map { it.amount }.assertEquals(expectedAnswer)
    }

    @Test
    fun `As an owner of an account, I can delete it`() {
        val otherUser = userRepository.register("jojo",  "test") as User
        connectUser(otherUser)
        val element = Booklet( Amount.fromString("100", "€".asCurrency()), "test", owner = user, id = 50L)
        accountState.init(listOf(
            AccountByOwner(listOf(element), otherUser.id)
        ))

        bookletFeature.deleteAccountById(element.id!!, session.tokenValue).assertTrue {
            val accounts = accountState.getStates()

            val expectedAccountSize = 0
            val actualAccountSize = accounts.find { it.userId == otherUser.id }?.booklet?.size ?: throw Error()
            expectedAccountSize == actualAccountSize
        }

        val accounts = accountState.getStates()
        val ofUser = accounts.find { it.userId == otherUser.id }!!

        assertNull(ofUser.existsById(50))
    }

    @Test
    fun `As an account's owner, I can retrieve it by its label`() {
        launchWithConnectedUserInstance {
            val element = Booklet(Amount.fromString("100", "€".asCurrency()), "test22", owner = Companion.user, id = 50L)
            accountState.init(listOf(
                AccountByOwner(listOf(element), this.user.id)
            ))
            bookletFeature.findByLabelAndUserId(tokenValue, element.label)
                .assertTrue {
                    this.label == "test22" && this.amount == Amount(100)
                }
        }
    }

    @Test
    fun `As an account's owner,  I can retrieve All of my Registered Accounts`() {
        launchWithConnectedUserWithoutAccount {
            val booklet = Booklet(Amount.fromString("100", "€".asCurrency()), "test1", owner = user, id = 50L)
            val booklet2 = Booklet(Amount.fromString("100", "€".asCurrency()), "test2", owner = user, id = 51L)
            val booklet3 = Booklet( Amount.fromString("100", "€".asCurrency()), "test3", owner = user, id= 52L)
            val booklet4 = Booklet( Amount.fromString("100", "€".asCurrency()), "test4", owner = user, id = 53L)
            val expectedAccount = listOf(
                booklet,
                booklet2,
                booklet3,
                booklet4
            )
            accountState.init(listOf(
                AccountByOwner(expectedAccount, userId)
            ))

            bookletFeature.findAllRegisteredAccounts(tokenValue)
                .assertContainsAtPosition(0, booklet)
                .assertContainsAtPosition(1, booklet2)
                .assertContainsAtPosition(2, booklet3)
                .assertContainsAtPosition(3, booklet4)
        }

    }

    @Test
    fun `As a Jmanager user, I can create new account`() {
        launchWithConnectedUserWithoutAccount {
            val bookletToSave = Booklet( Amount.fromString("100", "€".asCurrency()), "test1", owner = user, id = 50L)

            bookletFeature.save(tokenValue, bookletToSave)
                .assertTrue {
                    val expectedAmount = Amount(100)
                    val expectedLabelAccount = "test1"
                    this.amount == expectedAmount && this.label == expectedLabelAccount
                }
        }


    }

    @Test
    fun `As an account's owner, I cannot register an existing account with the same label`() {
        val otherUser = userRepository.register("jojo","test") as User
        connectUser(otherUser)

        accountState.init(listOf(
            AccountByOwner(listOf(Booklet(Amount.fromString("100", "€".asCurrency()), "test1", owner = otherUser, id = 50L)), otherUser.id)
        ))

        val bookletToSave = Booklet( Amount.fromString("150", "€".asCurrency()), "test1", owner = otherUser, id = 51L)
        bookletFeature.save(session.tokenValue, bookletToSave)
            .assertFailure()
    }
}