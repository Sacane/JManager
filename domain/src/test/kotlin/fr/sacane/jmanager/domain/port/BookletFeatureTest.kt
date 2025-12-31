package fr.sacane.jmanager.domain.port

import fr.sacane.jmanager.domain.*
import fr.sacane.jmanager.domain.fake.AccountByOwner
import fr.sacane.jmanager.domain.fake.FakeFactory
import fr.sacane.jmanager.domain.fake.IdUserAccount
import fr.sacane.jmanager.domain.fake.IdUserAccountByTransaction
import fr.sacane.jmanager.domain.fake.UserRegularTransaction
import fr.sacane.jmanager.domain.models.*
import fr.sacane.jmanager.domain.models.transaction.Transaction
import fr.sacane.jmanager.domain.models.transaction.regular.FrequencyProperty
import fr.sacane.jmanager.domain.models.transaction.regular.RecurrenceRule
import fr.sacane.jmanager.domain.models.transaction.regular.RegularTransaction
import fr.sacane.jmanager.domain.models.transaction.regular.RegularTransactionId
import fr.sacane.jmanager.domain.port.api.BookletFeature
import fr.sacane.jmanager.domain.port.spi.UserRepository
import fr.sacane.jmanager.domain.utils.Result
import fr.sacane.jmanager.domain.utils.ResultState
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.LocalDate
import java.util.*
import kotlin.collections.emptyList

class BookletFeatureTest: FeatureTest() {

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
        private val element = Booklet(Amount.fromString("100", "€".asCurrency()), "test", owner = user, id = UUID.randomUUID())
        override val action: List<Result<out Any>>
            get() = listOf(
                bookletFeature.findAccountById(UUID.randomUUID(), UUID.randomUUID().toString()),
                bookletFeature.save(UUID.randomUUID().toString(), element)
            )
    }

    @Test
    fun `Should find booklet by its Id`() {
        connectUser(user)
        val id = UUID.randomUUID()
        val element = Booklet(Amount.fromString("100", "€".asCurrency()), "test", owner = user, id= id)
        accountState.init(listOf(
            AccountByOwner(listOf(element), user.id)
        ))
        bookletFeature.findAccountById(id, session.tokenValue)
            .assertTrue {
                this.label == "test"
            }
    }

    @Test
    fun `Given an existing booklet it could be edit`() {
        connectUser(user)
        val element = Booklet(Amount.fromString("100", "€".asCurrency()), "test", owner = user, id = UUID.randomUUID())
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
    fun `As an owner of an booklet, I can delete it`() {
        val otherUser = userRepository.register("jojo",  "test") as User
        connectUser(otherUser)
        val element = Booklet( Amount.fromString("100", "€".asCurrency()), "test", owner = user, id = UUID.randomUUID())
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

        assertNull(ofUser.existsById(UUID.randomUUID()))
    }

    @Test
    fun `As an booklet's owner, I can retrieve it by its label`() {
        launchWithConnectedUserInstance {
            val element = Booklet(Amount.fromString("100", "€".asCurrency()), "test22", owner = Companion.user, id = UUID.randomUUID())
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
    fun `As an account's owner,  I can retrieve All of my Registered Booklets`() {
        launchWithConnectedUserWithoutAccount {

            val booklet = Booklet(Amount.fromString("100", "€".asCurrency()), "test1", owner = user, id = UUID.randomUUID())
            val booklet2 = Booklet(Amount.fromString("100", "€".asCurrency()), "test2", owner = user, id = UUID.randomUUID())
            val booklet3 = Booklet( Amount.fromString("100", "€".asCurrency()), "test3", owner = user, id= UUID.randomUUID())
            val booklet4 = Booklet( Amount.fromString("100", "€".asCurrency()), "test4", owner = user, id = UUID.randomUUID())
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
    fun `As a Jmanager user, I can create new booklet`() {
        launchWithConnectedUserWithoutAccount {
            val bookletToSave = Booklet( Amount.fromString("100", "€".asCurrency()), "test1", owner = user, id = UUID.randomUUID())

            bookletFeature.save(tokenValue, bookletToSave)
                .assertTrue {
                    val expectedAmount = Amount(100)
                    val expectedLabelAccount = "test1"
                    this.amount == expectedAmount && this.label == expectedLabelAccount
                }
        }
    }

    @Test
    fun `As a simple user, I cannot create more than six booklets`() {
        launchWithConnectedUserWithoutAccount {
            val bookletLists = mutableListOf<Booklet>()
            repeat(6) {
                val bookletToSave = Booklet( Amount.fromString("100", "€".asCurrency()), "test$it", owner = user, id = UUID.randomUUID())
                bookletLists.add(bookletToSave)
            }
            accountState.init(listOf(
                AccountByOwner(bookletLists, userId)
            ))

            bookletFeature.save(tokenValue, Booklet( Amount.fromString("100", "€".asCurrency()), "test7", owner = user, id = UUID.randomUUID()))
                .assertFailure(ResultState.BOOKLET_MAXIMUM_SIZE_REACHED)
        }
    }

    @Test
    fun `As an account's owner, I cannot register an existing booklet with the same label`() {
        val otherUser = userRepository.register("jojo","test") as User
        connectUser(otherUser)

        accountState.init(listOf(
            AccountByOwner(listOf(Booklet(Amount.fromString("100", "€".asCurrency()), "test1", owner = otherUser, id = UUID.randomUUID())), otherUser.id)
        ))

        val bookletToSave = Booklet( Amount.fromString("150", "€".asCurrency()), "test1", owner = otherUser, id = UUID.randomUUID())
        bookletFeature.save(session.tokenValue, bookletToSave)
            .assertFailure()
    }

    @Nested
    inner class LoadTransactionsForBookletForAMonthTest {

        @Test
        fun `Should calculate real sold correctly with income transactions only`() {
            launchWithConnectedUserInstance {
                val bookletId = UUID.randomUUID()
                val booklet = Booklet(
                    amount = 1000.toAmount(),
                    labelAccount = "Test Account",
                    owner = user.toUser(),
                    id = bookletId
                )

                accountState.init(listOf(AccountByOwner(listOf(booklet), user.id)))

                val transaction1 = Transaction(
                    id = UUID.randomUUID(),
                    label = "Salary",
                    date = java.time.LocalDate.of(2025, 1, 15),
                    amount = 2000.toAmount(),
                    isIncome = true,
                    isPreview = false
                )
                val transaction2 = Transaction(
                    id = UUID.randomUUID(),
                    label = "Bonus",
                    date = java.time.LocalDate.of(2025, 1, 20),
                    amount = 500.toAmount(),
                    isIncome = true,
                    isPreview = false
                )
                FakeFactory.fakeTransactionRepository()
                    .init(listOf(IdUserAccountByTransaction(IdUserAccount(user.id, bookletId), mutableListOf(transaction1))))
                FakeFactory.fakeTransactionRepository()
                    .init(listOf(IdUserAccountByTransaction(IdUserAccount(user.id, bookletId), mutableListOf(transaction2))))


                FakeFactory.regularTransactionState.init(emptyList())

                val result = bookletFeature.loadTransactionsForBookletForAMonth(
                    tokenValue,
                    bookletId,
                    java.time.Month.JANUARY,
                    2025,
                    startingMonth = java.time.Month.JANUARY,
                    startingYear = 2025
                )

                result.assertTrue { this.realSold == 3500.toAmount() } // 1000 + 2000 + 500
                result.assertTrue { this.currentTransactions.size == 2 }
                result.assertTrue { this.currentTransactions.all { it.isIncome } }
            }
        }

        @Test
        fun `Should calculate real sold correctly with expense transactions only`() {
            launchWithConnectedUserInstance {
                val bookletId = UUID.randomUUID()
                val booklet = Booklet(
                    amount = 1000.toAmount(),
                    labelAccount = "Test Account",
                    owner = user.toUser(),
                    id = bookletId
                )

                accountState.init(listOf(AccountByOwner(listOf(booklet), user.id)))

                val transaction1 = Transaction(
                    id = UUID.randomUUID(),
                    label = "Rent",
                    date = java.time.LocalDate.of(2025, 1, 5),
                    amount = 500.toAmount(),
                    isIncome = false,
                    isPreview = false
                )
                val transaction2 = Transaction(
                    id = UUID.randomUUID(),
                    label = "Groceries",
                    date = java.time.LocalDate.of(2025, 1, 10),
                    amount = 200.toAmount(),
                    isIncome = false,
                    isPreview = false
                )
                FakeFactory.fakeTransactionRepository()
                    .init(
                        listOf(
                            IdUserAccountByTransaction(
                                IdUserAccount(user.id, bookletId),
                                mutableListOf(transaction1, transaction2)
                            )
                        )
                    )

                FakeFactory.regularTransactionState.init(emptyList())

                val result = bookletFeature.loadTransactionsForBookletForAMonth(
                    tokenValue,
                    bookletId,
                    java.time.Month.JANUARY,
                    2025,
                    startingMonth = java.time.Month.JANUARY,
                    startingYear = 2025
                )

                result.assertTrue { this.realSold == 300.toAmount() } // 1000 - 500 - 200
                result.assertTrue { this.currentTransactions.size == 2 }
                result.assertTrue { this.currentTransactions.none { it.isIncome } }
            }
        }

        @Test
        fun `Should calculate real sold correctly with mixed income and expense transactions`() {
            launchWithConnectedUserInstance {
                val bookletId = UUID.randomUUID()
                val booklet = Booklet(
                    amount = 1000.toAmount(),
                    labelAccount = "Mixed Account",
                    owner = user.toUser(),
                    id = bookletId
                )

                accountState.init(listOf(AccountByOwner(listOf(booklet), user.id)))

                val transaction1 = Transaction(
                    id = UUID.randomUUID(),
                    label = "Income 1",
                    date = java.time.LocalDate.of(2025, 2, 5),
                    amount = 1500.toAmount(),
                    isIncome = true,
                    isPreview = false
                )
                val transaction2 = Transaction(
                    id = UUID.randomUUID(),
                    label = "Expense 1",
                    date = java.time.LocalDate.of(2025, 2, 10),
                    amount = 300.toAmount(),
                    isIncome = false,
                    isPreview = false
                )
                val transaction3 = Transaction(
                    id = UUID.randomUUID(),
                    label = "Income 2",
                    date = java.time.LocalDate.of(2025, 2, 15),
                    amount = 800.toAmount(),
                    isIncome = true,
                    isPreview = false
                )
                val transaction4 = Transaction(
                    id = UUID.randomUUID(),
                    label = "Expense 2",
                    date = java.time.LocalDate.of(2025, 2, 20),
                    amount = 500.toAmount(),
                    isIncome = false,
                    isPreview = false
                )

                FakeFactory.fakeTransactionRepository()
                    .init(
                        listOf(
                            IdUserAccountByTransaction(
                                IdUserAccount(user.id, bookletId),
                                mutableListOf(
                                    transaction1, transaction2, transaction3, transaction4
                                )
                            )
                        )
                    )
                FakeFactory.regularTransactionState.init(emptyList())

                val result = bookletFeature.loadTransactionsForBookletForAMonth(
                    tokenValue,
                    bookletId,
                    java.time.Month.FEBRUARY,
                    2025,
                    startingMonth = java.time.Month.FEBRUARY,
                    startingYear = 2025
                )
                result.assertTrue { this.realSold == 2500.toAmount() } // 1000 + 1500 - 300 + 800 - 500 = 2500
            }
        }

        @Test
        fun `Should calculate previsional sold correctly with preview transactions`() {
            val bookletId = UUID.randomUUID()
            launchWithConnectedUserInstance {
                val booklet = Booklet(
                    amount = 1000.toAmount(),
                    labelAccount = "Preview Account",
                    owner = user.toUser(),
                    id = bookletId
                )

                accountState.init(listOf(AccountByOwner(listOf(booklet), user.id)))

                val transaction1 = Transaction(
                    id = UUID.randomUUID(),
                    label = "Current Income",
                    date = java.time.LocalDate.of(2025, 11, 5),
                    amount = 500.toAmount(),
                    isIncome = true,
                    isPreview = false
                )
                val transaction2 = Transaction(
                    id = UUID.randomUUID(),
                    label = "Future Income",
                    date = java.time.LocalDate.of(2025, 11, 25),
                    amount = 1000.toAmount(),
                    isIncome = true,
                    isPreview = true
                )
                val transaction3 = Transaction(
                    id = UUID.randomUUID(),
                    label = "Future Expense",
                    date = java.time.LocalDate.of(2025, 11, 28),
                    amount = 300.toAmount(),
                    isIncome = false,
                    isPreview = true
                )
                FakeFactory.fakeTransactionRepository()
                    .init(
                        listOf(
                            IdUserAccountByTransaction(
                                IdUserAccount(user.id, bookletId),
                                mutableListOf(transaction1, transaction2, transaction3)
                            )
                        )
                    )
                FakeFactory.regularTransactionState.init(emptyList())

                val result = bookletFeature.loadTransactionsForBookletForAMonth(
                    tokenValue,
                    bookletId,
                    java.time.Month.NOVEMBER,
                    2025,
                    startingMonth = java.time.Month.NOVEMBER,
                    startingYear = 2025
                )

                result.assertTrue { this.realSold == 1500.toAmount() } // 1000 + 500 (only current)
                result.assertTrue { this.previsionalSold.value > this.realSold.value } // Should be higher with preview
                result.assertTrue { this.previsionalTransactions.size == 2 }
                result.assertTrue { this.currentTransactions.size == 1 }
            }
        }

        @Test
        fun `Should separate current and previsional transactions correctly`() {
            launchWithConnectedUserInstance {
                val bookletId = UUID.randomUUID()
                val booklet = Booklet(
                    amount = 2000.toAmount(),
                    labelAccount = "Separation Test",
                    owner = user.toUser(),
                    id = bookletId
                )

                accountState.init(listOf(AccountByOwner(listOf(booklet), user.id)))

                val transaction1 = Transaction(
                    id = UUID.randomUUID(),
                    label = "Current 1",
                    date = java.time.LocalDate.of(2025, 4, 1),
                    amount = 100.toAmount(),
                    isIncome = true,
                    isPreview = false
                )
                val transaction2 = Transaction(
                    id = UUID.randomUUID(),
                    label = "Current 2",
                    date = java.time.LocalDate.of(2025, 4, 10),
                    amount = 50.toAmount(),
                    isIncome = false,
                    isPreview = false
                )
                val transaction3 = Transaction(
                    id = UUID.randomUUID(),
                    label = "Current 3",
                    date = java.time.LocalDate.of(2025, 4, 15),
                    amount = 75.toAmount(),
                    isIncome = true,
                    isPreview = false
                )
                val transaction4 = Transaction(
                    id = UUID.randomUUID(),
                    label = "Preview 1",
                    date = java.time.LocalDate.of(2025, 4, 20),
                    amount = 200.toAmount(),
                    isIncome = true,
                    isPreview = true
                )
                val transaction5 = Transaction(
                    id = UUID.randomUUID(),
                    label = "Preview 2",
                    date = java.time.LocalDate.of(2025, 4, 25),
                    amount = 150.toAmount(),
                    isIncome = false,
                    isPreview = true
                )

                FakeFactory.fakeTransactionRepository()
                    .init(
                        listOf(
                            IdUserAccountByTransaction(
                                IdUserAccount(user.id, bookletId),
                                mutableListOf(
                                    transaction1, transaction2, transaction3, transaction4, transaction5
                                )
                            )
                        )
                    )
                FakeFactory.regularTransactionState.init(emptyList())

                val result = bookletFeature.loadTransactionsForBookletForAMonth(
                    tokenValue,
                    bookletId,
                    java.time.Month.APRIL,
                    2025,
                    startingMonth = java.time.Month.APRIL,
                    startingYear = 2025
                )

                result.assertTrue { this.currentTransactions.size == 3 }
                result.assertTrue { this.previsionalTransactions.size == 2 }
                result.assertTrue { this.currentTransactions.all { !it.isPreview } }
                result.assertTrue { this.previsionalTransactions.all { it.isPreview } }
            }
        }

        @Test
        fun `Should retrieve regular transactions for the booklet`() {
            launchWithConnectedUserInstance {
                val bookletId = UUID.randomUUID()
                val booklet = Booklet(
                    amount = 1000.toAmount(),
                    labelAccount = "Regular Account",
                    owner = user.toUser(),
                    id = bookletId
                )

                accountState.init(listOf(AccountByOwner(listOf(booklet), user.id)))

                val regularTransaction1 = RegularTransaction(
                    label = "Monthly Salary",
                    amount = 3000.toAmount(),
                    isIncome = true,
                    id = RegularTransactionId("${user.id.value}-salary"),
                    startDate = LocalDate.of(2025, 1, 1),
                    frequencyProperty = FrequencyProperty.Forever(),
                    recurrenceRule = RecurrenceRule.Monthly(1)
                )

                val regularTransaction2 = RegularTransaction(
                    label = "Monthly Rent",
                    amount = 800.toAmount(),
                    isIncome = false,
                    id = RegularTransactionId("${user.id.value}-rent"),
                    startDate = LocalDate.of(2025, 1, 5),
                    frequencyProperty = FrequencyProperty.Forever(),
                    recurrenceRule = RecurrenceRule.Monthly(5)
                )

                FakeFactory.regularTransactionState.init(
                    listOf(
                        UserRegularTransaction(userId = user.id, transaction = regularTransaction1, bookletIds = listOf(bookletId)),
                        UserRegularTransaction(userId = user.id, transaction = regularTransaction2, bookletIds = listOf(bookletId))
                    )
                )

                val result = bookletFeature.loadTransactionsForBookletForAMonth(
                    tokenValue,
                    bookletId,
                    java.time.Month.JANUARY,
                    2025,
                    startingMonth = java.time.Month.JANUARY,
                    startingYear = 2025
                )

                result.assertTrue { this.regularTransactions.size == 2 }
                result.assertTrue { this.regularTransactions.any { it.label == "Monthly Salary" } }
                result.assertTrue { this.regularTransactions.any { it.label == "Monthly Rent" } }
            }
        }

        @Test
        fun `Should return empty lists when booklet has no transactions`() {
            launchWithConnectedUserInstance {
                val bookletId = UUID.randomUUID()
                val booklet = Booklet(
                    amount = 500.toAmount(),
                    labelAccount = "Empty Account",
                    owner = user.toUser(),
                    id = bookletId
                )

                accountState.init(listOf(AccountByOwner(listOf(booklet), user.id)))

                FakeFactory.regularTransactionState.init(emptyList())

                val result = bookletFeature.loadTransactionsForBookletForAMonth(
                    tokenValue,
                    bookletId,
                    java.time.Month.MAY,
                    2025,
                    startingMonth = java.time.Month.MAY,
                    startingYear = 2025
                )

                result.assertTrue { this.currentTransactions.isEmpty() }
                result.assertTrue { this.previsionalTransactions.isEmpty() }
                result.assertTrue { this.realSold == 500.toAmount() }
                result.assertTrue { this.label == "Empty Account" }
            }
        }

        @Test
        fun `Should fail when booklet does not exist`() {
            launchWithConnectedUserInstance {
                val result = bookletFeature.loadTransactionsForBookletForAMonth(
                    tokenValue,
                    UUID.randomUUID(), // Non-existent booklet ID
                    java.time.Month.JUNE,
                    2025,
                    startingMonth = java.time.Month.JUNE,
                    startingYear = 2025
                )

                result.assertFailure()
            }
        }

        @Test
        fun `Should only load transactions for the requested month and year`() {
            launchWithConnectedUserInstance {
                val bookletId = UUID.randomUUID()
                val booklet = Booklet(
                    amount = 1000.toAmount(),
                    labelAccount = "Month Filter Test",
                    owner = user.toUser(),
                    id = bookletId
                )

                accountState.init(listOf(AccountByOwner(listOf(booklet), user.id)))

                val transaction1 = Transaction(
                    id = UUID.randomUUID(),
                    label = "January Transaction",
                    date = LocalDate.of(2025, 1, 15),
                    amount = 100.toAmount(),
                    isIncome = true,
                    isPreview = false
                )
                val transaction2 = Transaction(
                    id = UUID.randomUUID(),
                    label = "February Transaction 1",
                    date = java.time.LocalDate.of(2025, 2, 10),
                    amount = 200.toAmount(),
                    isIncome = true,
                    isPreview = false
                )
                val transaction3 = Transaction(
                    id = UUID.randomUUID(),
                    label = "February Transaction 2",
                    date = java.time.LocalDate.of(2025, 2, 20),
                    amount = 150.toAmount(),
                    isIncome = false,
                    isPreview = false
                )
                val transaction4 = Transaction(
                    id = UUID.randomUUID(),
                    label = "March Transaction",
                    date = java.time.LocalDate.of(2025, 3, 5),
                    amount = 50.toAmount(),
                    isIncome = true,
                    isPreview = false
                )

                FakeFactory.fakeTransactionRepository()
                    .init(
                        listOf(
                            IdUserAccountByTransaction(
                                IdUserAccount(user.id, bookletId),
                                mutableListOf(
                                    transaction1, transaction2, transaction3, transaction4
                                )
                            )
                        )
                    )
                FakeFactory.regularTransactionState.init(emptyList())

                val result = bookletFeature.loadTransactionsForBookletForAMonth(
                    tokenValue,
                    bookletId,
                    java.time.Month.FEBRUARY,
                    2025,
                    startingMonth = java.time.Month.FEBRUARY,
                    startingYear = 2025
                )

                result.assertTrue { this.currentTransactions.size == 2 }
                result.assertTrue { this.currentTransactions.all { it.date.month == java.time.Month.FEBRUARY } }
                result.assertTrue { this.currentTransactions.all { it.date.year == 2025 } }
                result.assertTrue { this.currentTransactions.any { it.label == "February Transaction 1" } }
                result.assertTrue { this.currentTransactions.any { it.label == "February Transaction 2" } }
            }
        }

        @Test
        fun `Should calculate previsional sold including future months transactions`() {
            launchWithConnectedUserInstance {
                val bookletId = UUID.randomUUID()
                val booklet = Booklet(
                    amount = 1000.toAmount(),
                    labelAccount = "Future Sold Test",
                    owner = user.toUser(),
                    id = bookletId
                )

                accountState.init(listOf(AccountByOwner(listOf(booklet), user.id)))

                val transaction1 = Transaction(
                    id = UUID.randomUUID(),
                    label = "Current Income",
                    date = java.time.LocalDate.of(2025, 11, 15),
                    amount = 500.toAmount(),
                    isIncome = true,
                    isPreview = false
                )
                val transaction2 = Transaction(
                    id = UUID.randomUUID(),
                    label = "Future Income",
                    date = java.time.LocalDate.of(2025, 12, 10),
                    amount = 800.toAmount(),
                    isIncome = true,
                    isPreview = true
                )
                val transaction3 = Transaction(
                    id = UUID.randomUUID(),
                    label = "Future Expense",
                    date = java.time.LocalDate.of(2026, 1, 5),
                    amount = 300.toAmount(),
                    isIncome = false,
                    isPreview = true
                )

                FakeFactory.fakeTransactionRepository()
                    .init(
                        listOf(
                            IdUserAccountByTransaction(
                                IdUserAccount(user.id, bookletId),
                                mutableListOf(
                                    transaction1, transaction2, transaction3
                                )
                            )
                        )
                    )
                FakeFactory.regularTransactionState.init(emptyList())

                val result = bookletFeature.loadTransactionsForBookletForAMonth(
                    tokenValue,
                    bookletId,
                    java.time.Month.JANUARY,
                    2026,
                    startingMonth = java.time.Month.NOVEMBER,
                    startingYear = 2025
                )

                result.assertTrue { this.realSold == 1500.toAmount() } // 1000 + 500
                result.assertTrue { this.previsionalSold.value > this.realSold.value }
            }
        }

        @Test
        fun `Should reflect add and remove of regular transaction via regularTransactionState`() {
            launchWithConnectedUserInstance {
                val bookletId = UUID.randomUUID()
                val booklet = Booklet(
                    amount = 1000.toAmount(),
                    labelAccount = "RT CRUD Account",
                    owner = user.toUser(),
                    id = bookletId
                )
                accountState.init(listOf(AccountByOwner(listOf(booklet), user.id)))

                FakeFactory.regularTransactionState.init(emptyList())

                var result = bookletFeature.loadTransactionsForBookletForAMonth(
                    tokenValue, bookletId, java.time.Month.JANUARY, 2025,
                    startingMonth = java.time.Month.JANUARY,
                    startingYear = 2025
                )
                result.assertTrue { this.regularTransactions.isEmpty() }

                val regular = RegularTransaction(
                    label = "Monthly Income RT",
                    amount = 100.toAmount(),
                    isIncome = true,
                    id = RegularTransactionId("${user.id.value}-rt1"),
                    startDate = LocalDate.of(2025, 1, 1),
                    frequencyProperty = FrequencyProperty.Forever(),
                    recurrenceRule = RecurrenceRule.Monthly(1)
                )
                FakeFactory.regularTransactionState.init(
                    listOf(
                        UserRegularTransaction(userId = user.id, transaction = regular, bookletIds = listOf(bookletId))
                    )
                )

                result = bookletFeature.loadTransactionsForBookletForAMonth(
                    tokenValue, bookletId, java.time.Month.JANUARY, 2025,
                    startingMonth = java.time.Month.JANUARY,
                    startingYear = 2025
                )
                result.assertTrue { this.regularTransactions.size == 1 }
                result.assertTrue { this.regularTransactions.any { it.label == "Monthly Income RT" } }

                FakeFactory.regularTransactionState.init(emptyList())
                result = bookletFeature.loadTransactionsForBookletForAMonth(
                    tokenValue, bookletId, java.time.Month.JANUARY, 2025,
                    startingMonth = java.time.Month.JANUARY,
                    startingYear = 2025
                )
                result.assertTrue { this.regularTransactions.isEmpty() }
            }
        }

        @Test
        fun `Should not expose regular transactions of other user (multi-tenant isolation)`() {
            launchWithConnectedUserInstance {
                val bookletIdMine = UUID.randomUUID()
                val bookletMine = Booklet(1000.toAmount(), "My Account", owner = user.toUser(), id = bookletIdMine)
                val otherUser = userRepository.register("other${UUID.randomUUID()}", "pw") as User
                val bookletIdOther = UUID.randomUUID()
                val bookletOther = Booklet(1000.toAmount(), "Other Account", owner = otherUser, id = bookletIdOther)

                accountState.init(listOf(
                    AccountByOwner(listOf(bookletMine), user.id),
                    AccountByOwner(listOf(bookletOther), otherUser.id)
                ))

                val rtMine = RegularTransaction(
                    label = "Mine RT",
                    amount = 50.toAmount(),
                    isIncome = true,
                    id = RegularTransactionId("${user.id.value}-mine"),
                    startDate = LocalDate.of(2025, 1, 1),
                    frequencyProperty = FrequencyProperty.Forever(),
                    recurrenceRule = RecurrenceRule.Monthly(1)
                )
                val rtOther = RegularTransaction(
                    label = "Other RT",
                    amount = 999.toAmount(),
                    isIncome = true,
                    id = RegularTransactionId("${otherUser.id.value}-other"),
                    startDate = LocalDate.of(2025, 1, 1),
                    frequencyProperty = FrequencyProperty.Forever(),
                    recurrenceRule = RecurrenceRule.Monthly(1)
                )

                FakeFactory.regularTransactionState.init(
                    listOf(
                        UserRegularTransaction(userId = user.id, transaction = rtMine, bookletIds = listOf(bookletIdMine)),
                        UserRegularTransaction(userId = otherUser.id, transaction = rtOther, bookletIds = listOf(bookletIdOther))
                    )
                )

                val result = bookletFeature.loadTransactionsForBookletForAMonth(
                    tokenValue, bookletIdMine, java.time.Month.JANUARY, 2025,
                    startingMonth = java.time.Month.JANUARY,
                    startingYear = 2025
                )

                result.assertTrue { this.regularTransactions.size == 1 }
                result.assertTrue { this.regularTransactions.any { it.label == "Mine RT" } }
                result.assertTrue { this.regularTransactions.none { it.label == "Other RT" } }
            }
        }

        @Test
        fun `Should include regular transactions in previsional sold calculation`() {
            launchWithConnectedUserInstance {
                val bookletId = UUID.randomUUID()
                val booklet = Booklet(
                    amount = 1000.toAmount(),
                    labelAccount = "RT Calculation Account",
                    owner = user.toUser(),
                    id = bookletId
                )
                accountState.init(listOf(AccountByOwner(listOf(booklet), user.id)))

                val regularIncome = RegularTransaction(
                    label = "Monthly Salary",
                    amount = 2000.toAmount(),
                    isIncome = true,
                    id = RegularTransactionId("${user.id.value}-salary"),
                    startDate = LocalDate.of(2025, 11, 1),
                    frequencyProperty = FrequencyProperty.Forever(),
                    recurrenceRule = RecurrenceRule.Monthly(1)
                )

                FakeFactory.regularTransactionState.init(
                    listOf(UserRegularTransaction(userId = user.id, transaction = regularIncome, bookletIds = listOf(bookletId)))
                )

                val result = bookletFeature.loadTransactionsForBookletForAMonth(
                    tokenValue, bookletId, java.time.Month.NOVEMBER, 2025,
                    startingMonth = java.time.Month.NOVEMBER,
                    startingYear = 2025
                )
                println(result.mapNotNullOrFailure())
                result.assertTrue { this.regularTransactions.size == 1 }
                result.assertTrue { this.previsionalSold.value >= BigDecimal(3000) }
            }
        }

        @Test
        fun `Should handle multiple regular transactions with different frequencies`() {
            launchWithConnectedUserInstance {
                val bookletId = UUID.randomUUID()
                val booklet = Booklet(
                    amount = 1000.toAmount(),
                    labelAccount = "Multi RT Account",
                    owner = user.toUser(),
                    id = bookletId
                )
                accountState.init(listOf(AccountByOwner(listOf(booklet), user.id)))

                val rt1 = RegularTransaction(
                    label = "RT Start Day 1",
                    amount = 500.toAmount(),
                    isIncome = true,
                    id = RegularTransactionId("${user.id.value}-rt1"),
                    startDate = LocalDate.of(2025, 1, 1),
                    frequencyProperty = FrequencyProperty.Forever(),
                    recurrenceRule = RecurrenceRule.Monthly(1)
                )
                val rt2 = RegularTransaction(
                    label = "RT Start Day 15",
                    amount = 300.toAmount(),
                    isIncome = false,
                    id = RegularTransactionId("${user.id.value}-rt2"),
                    startDate = LocalDate.of(2025, 1, 15),
                    frequencyProperty = FrequencyProperty.Forever(),
                    recurrenceRule = RecurrenceRule.Monthly(15)
                )
                val rt3 = RegularTransaction(
                    label = "RT Start Day 25",
                    amount = 200.toAmount(),
                    isIncome = true,
                    id = RegularTransactionId("${user.id.value}-rt3"),
                    startDate = LocalDate.of(2025, 1, 25),
                    frequencyProperty = FrequencyProperty.Forever(),
                    recurrenceRule = RecurrenceRule.Monthly(25)
                )

                FakeFactory.regularTransactionState.init(
                    listOf(
                        UserRegularTransaction(userId = user.id, transaction = rt1, bookletIds = listOf(bookletId)),
                        UserRegularTransaction(userId = user.id, transaction = rt2, bookletIds = listOf(bookletId)),
                        UserRegularTransaction(userId = user.id, transaction = rt3, bookletIds = listOf(bookletId))
                    )
                )

                val result = bookletFeature.loadTransactionsForBookletForAMonth(
                    tokenValue, bookletId, java.time.Month.JANUARY, 2025,
                    startingMonth = java.time.Month.JANUARY,
                    startingYear = 2025
                )

                result.assertTrue { this.regularTransactions.size == 3 }
                result.assertTrue { this.regularTransactions.any { it.label == "RT Start Day 1" } }
                result.assertTrue { this.regularTransactions.any { it.label == "RT Start Day 15" } }
                result.assertTrue { this.regularTransactions.any { it.label == "RT Start Day 25" } }
            }
        }

        @Test
        fun `Should combine regular transactions with current and preview transactions correctly`() {
            launchWithConnectedUserInstance {
                val bookletId = UUID.randomUUID()
                val booklet = Booklet(
                    amount = 1000.toAmount(),
                    labelAccount = "Combined Account",
                    owner = user.toUser(),
                    id = bookletId
                )
                accountState.init(listOf(AccountByOwner(listOf(booklet), user.id)))

                val currentTx = Transaction(
                    id = UUID.randomUUID(),
                    label = "Current Expense",
                    date = LocalDate.of(2025, 12, 5),
                    amount = 100.toAmount(),
                    isIncome = false,
                    isPreview = false
                )
                val previewTx = Transaction(
                    id = UUID.randomUUID(),
                    label = "Preview Income",
                    date = LocalDate.of(2025, 12, 20),
                    amount = 500.toAmount(),
                    isIncome = true,
                    isPreview = true
                )

                FakeFactory.fakeTransactionRepository().init(
                    listOf(
                        IdUserAccountByTransaction(
                            IdUserAccount(user.id, bookletId),
                            mutableListOf(currentTx, previewTx)
                        )
                    )
                )

                val regularTx = RegularTransaction(
                    label = "Regular Income",
                    amount = 2000.toAmount(),
                    isIncome = true,
                    id = RegularTransactionId("${user.id.value}-regular"),
                    startDate = LocalDate.of(2025, 10, 1),
                    frequencyProperty = FrequencyProperty.Forever(),
                    recurrenceRule = RecurrenceRule.Monthly(1)
                )

                FakeFactory.regularTransactionState.init(
                    listOf(UserRegularTransaction(userId = user.id, transaction = regularTx, bookletIds = listOf(bookletId)))
                )

                val result = bookletFeature.loadTransactionsForBookletForAMonth(
                    tokenValue, bookletId, java.time.Month.DECEMBER, 2025,
                    startingMonth = java.time.Month.DECEMBER,
                    startingYear = 2025
                )

                result.assertTrue { this.regularTransactions.size == 1 }
                result.assertTrue { this.regularTransactions.any { it.label == "Regular Income" } }

                result.assertTrue { this.currentTransactions.isNotEmpty() || this.previsionalTransactions.isNotEmpty() }

                result.assertTrue { this.previsionalSold.value >= this.realSold.value }
            }
        }

        @Test
        fun `Should not include regular transactions that started after the requested month`() {
            launchWithConnectedUserInstance {
                val bookletId = UUID.randomUUID()
                val booklet = Booklet(
                    amount = 1000.toAmount(),
                    labelAccount = "Future RT Account",
                    owner = user.toUser(),
                    id = bookletId
                )
                accountState.init(listOf(AccountByOwner(listOf(booklet), user.id)))

                val futureRT = RegularTransaction(
                    label = "Future RT",
                    amount = 500.toAmount(),
                    isIncome = true,
                    id = RegularTransactionId("${user.id.value}-future"),
                    startDate = LocalDate.of(2025, 3, 1),
                    frequencyProperty = FrequencyProperty.Forever(),
                    recurrenceRule = RecurrenceRule.Monthly(1)
                )

                FakeFactory.regularTransactionState.init(
                    listOf(UserRegularTransaction(userId = user.id, transaction = futureRT, bookletIds = listOf(bookletId)))
                )

                val result = bookletFeature.loadTransactionsForBookletForAMonth(
                    tokenValue, bookletId, java.time.Month.JANUARY, 2025
                )

                result.assertTrue { this.regularTransactions.isEmpty() }
            }
        }

    }
}