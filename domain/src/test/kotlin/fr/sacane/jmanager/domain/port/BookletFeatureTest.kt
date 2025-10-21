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
import fr.sacane.jmanager.domain.models.transaction.regular.MonthlyRepeatProperty
import fr.sacane.jmanager.domain.models.transaction.regular.MonthlyTransaction
import fr.sacane.jmanager.domain.models.transaction.regular.RegularTransactionId
import fr.sacane.jmanager.domain.port.api.BookletFeature
import fr.sacane.jmanager.domain.port.spi.UserRepository
import fr.sacane.jmanager.domain.utils.Result
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

    @Nested
    inner class LoadTransactionsForBookletForAMonthTest {

        @Test
        fun `Should calculate real sold correctly with income transactions only`() {
            launchWithConnectedUserInstance {
                val booklet = Booklet(
                    amount = 1000.toAmount(),
                    labelAccount = "Test Account",
                    owner = user.toUser(),
                    id = 100L
                )

                accountState.init(listOf(AccountByOwner(listOf(booklet), user.id)))

                // Add income transactions via the database
                val transaction1 = Transaction(
                    id = 1L,
                    label = "Salary",
                    date = java.time.LocalDate.of(2025, 1, 15),
                    amount = 2000.toAmount(),
                    isIncome = true,
                    isPreview = false
                )
                val transaction2 = Transaction(
                    id = 2L,
                    label = "Bonus",
                    date = java.time.LocalDate.of(2025, 1, 20),
                    amount = 500.toAmount(),
                    isIncome = true,
                    isPreview = false
                )

                FakeFactory.fakeTransactionRepository()
                    .init(listOf(IdUserAccountByTransaction(IdUserAccount(user.id, 100L), mutableListOf(transaction1))))
                FakeFactory.fakeTransactionRepository()
                    .init(listOf(IdUserAccountByTransaction(IdUserAccount(user.id, 100L), mutableListOf(transaction2))))


                // Initialize regular transactions (empty list is valid)
                FakeFactory.regularTransactionState.init(emptyList())

                val result = bookletFeature.loadTransactionsForBookletForAMonth(
                    tokenValue,
                    100L,
                    java.time.Month.JANUARY,
                    2025
                )

                result.assertTrue { this.realSold == 3500.toAmount() } // 1000 + 2000 + 500
                result.assertTrue { this.currentTransactions.size == 2 }
                result.assertTrue { this.currentTransactions.all { it.isIncome } }
            }
        }

        @Test
        fun `Should calculate real sold correctly with expense transactions only`() {
            launchWithConnectedUserInstance {
                val booklet = Booklet(
                    amount = 1000.toAmount(),
                    labelAccount = "Test Account",
                    owner = user.toUser(),
                    id = 101L
                )

                accountState.init(listOf(AccountByOwner(listOf(booklet), user.id)))

                // Add expense transactions via the database
                val transaction1 = Transaction(
                    id = 3L,
                    label = "Rent",
                    date = java.time.LocalDate.of(2025, 1, 5),
                    amount = 500.toAmount(),
                    isIncome = false,
                    isPreview = false
                )
                val transaction2 = Transaction(
                    id = 4L,
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
                                IdUserAccount(user.id, 101L),
                                mutableListOf(transaction1, transaction2)
                            )
                        )
                    )

                // Initialize regular transactions (empty list is valid)
                FakeFactory.regularTransactionState.init(emptyList())

                val result = bookletFeature.loadTransactionsForBookletForAMonth(
                    tokenValue,
                    101L,
                    java.time.Month.JANUARY,
                    2025
                )

                result.assertTrue { this.realSold == 300.toAmount() } // 1000 - 500 - 200
                result.assertTrue { this.currentTransactions.size == 2 }
                result.assertTrue { this.currentTransactions.none { it.isIncome } }
            }
        }

        @Test
        fun `Should calculate real sold correctly with mixed income and expense transactions`() {
            launchWithConnectedUserInstance {
                val booklet = Booklet(
                    amount = 1000.toAmount(),
                    labelAccount = "Mixed Account",
                    owner = user.toUser(),
                    id = 102L
                )

                accountState.init(listOf(AccountByOwner(listOf(booklet), user.id)))

                val transaction1 = Transaction(
                    id = 5L,
                    label = "Income 1",
                    date = java.time.LocalDate.of(2025, 2, 5),
                    amount = 1500.toAmount(),
                    isIncome = true,
                    isPreview = false
                )
                val transaction2 = Transaction(
                    id = 6L,
                    label = "Expense 1",
                    date = java.time.LocalDate.of(2025, 2, 10),
                    amount = 300.toAmount(),
                    isIncome = false,
                    isPreview = false
                )
                val transaction3 = Transaction(
                    id = 7L,
                    label = "Income 2",
                    date = java.time.LocalDate.of(2025, 2, 15),
                    amount = 800.toAmount(),
                    isIncome = true,
                    isPreview = false
                )
                val transaction4 = Transaction(
                    id = 8L,
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
                                IdUserAccount(user.id, 102L),
                                mutableListOf(
                                    transaction1, transaction2, transaction3, transaction4
                                )
                            )
                        )
                    )
                // Initialize regular transactions (empty list is valid)
                FakeFactory.regularTransactionState.init(emptyList())

                val result = bookletFeature.loadTransactionsForBookletForAMonth(
                    tokenValue,
                    102L,
                    java.time.Month.FEBRUARY,
                    2025
                )
                result.assertTrue { this.realSold == 2500.toAmount() } // 1000 + 1500 - 300 + 800 - 500 = 2500
            }

        @Test
        fun `Should calculate previsional sold correctly with preview transactions`() {
            launchWithConnectedUserInstance {
                val booklet = Booklet(
                    amount = 1000.toAmount(),
                    labelAccount = "Preview Account",
                    owner = user.toUser(),
                    id = 103L
                )

                accountState.init(listOf(AccountByOwner(listOf(booklet), user.id)))

                // Add current and preview transactions via the database
                val transaction1 = Transaction(
                    id = 9L,
                    label = "Current Income",
                    date = java.time.LocalDate.of(2025, 3, 5),
                    amount = 500.toAmount(),
                    isIncome = true,
                    isPreview = false
                )
                val transaction2 = Transaction(
                    id = 10L,
                    label = "Future Income",
                    date = java.time.LocalDate.of(2025, 3, 25),
                    amount = 1000.toAmount(),
                    isIncome = true,
                    isPreview = true
                )
                val transaction3 = Transaction(
                    id = 11L,
                    label = "Future Expense",
                    date = java.time.LocalDate.of(2025, 3, 28),
                    amount = 300.toAmount(),
                    isIncome = false,
                    isPreview = true
                )
                FakeFactory.fakeTransactionRepository()
                    .init(
                        listOf(
                            IdUserAccountByTransaction(
                                IdUserAccount(user.id, 103L),
                                mutableListOf(transaction1, transaction2, transaction3)
                            )
                        )
                    )
                // Initialize regular transactions (empty list is valid)
                FakeFactory.regularTransactionState.init(emptyList())

                val result = bookletFeature.loadTransactionsForBookletForAMonth(
                    tokenValue,
                    103L,
                    java.time.Month.MARCH,
                    2025
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
                val booklet = Booklet(
                    amount = 2000.toAmount(),
                    labelAccount = "Separation Test",
                    owner = user.toUser(),
                    id = 104L
                )

                accountState.init(listOf(AccountByOwner(listOf(booklet), user.id)))

                // Add 3 current transactions and 2 preview transactions via the database
                val transaction1 = Transaction(
                    id = 12L,
                    label = "Current 1",
                    date = java.time.LocalDate.of(2025, 4, 1),
                    amount = 100.toAmount(),
                    isIncome = true,
                    isPreview = false
                )
                val transaction2 = Transaction(
                    id = 13L,
                    label = "Current 2",
                    date = java.time.LocalDate.of(2025, 4, 10),
                    amount = 50.toAmount(),
                    isIncome = false,
                    isPreview = false
                )
                val transaction3 = Transaction(
                    id = 14L,
                    label = "Current 3",
                    date = java.time.LocalDate.of(2025, 4, 15),
                    amount = 75.toAmount(),
                    isIncome = true,
                    isPreview = false
                )
                val transaction4 = Transaction(
                    id = 15L,
                    label = "Preview 1",
                    date = java.time.LocalDate.of(2025, 4, 20),
                    amount = 200.toAmount(),
                    isIncome = true,
                    isPreview = true
                )
                val transaction5 = Transaction(
                    id = 16L,
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
                                IdUserAccount(user.id, 104L),
                                mutableListOf(
                                    transaction1, transaction2, transaction3, transaction4, transaction5
                                )
                            )
                        )
                    )
                // Initialize regular transactions (empty list is valid)
                FakeFactory.regularTransactionState.init(emptyList())

                val result = bookletFeature.loadTransactionsForBookletForAMonth(
                    tokenValue,
                    104L,
                    java.time.Month.APRIL,
                    2025
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
                val booklet = Booklet(
                    amount = 1000.toAmount(),
                    labelAccount = "Regular Account",
                    owner = user.toUser(),
                    id = 105L
                )

                accountState.init(listOf(AccountByOwner(listOf(booklet), user.id)))

                // Create regular transactions
                val regularTransaction1 = MonthlyTransaction(
                    label = "Monthly Salary",
                    amount = 3000.toAmount(),
                    isIncome = true,
                    id = RegularTransactionId("${user.id.value}-salary"),
                    startDate = LocalDate.of(2025, 1, 1),
                    frequencyProperty = FrequencyProperty.Forever(),
                    monthlyRepeatProperty = MonthlyRepeatProperty(1)
                )

                val regularTransaction2 = MonthlyTransaction(
                    label = "Monthly Rent",
                    amount = 800.toAmount(),
                    isIncome = false,
                    id = RegularTransactionId("${user.id.value}-rent"),
                    startDate = LocalDate.of(2025, 1, 5),
                    frequencyProperty = FrequencyProperty.Forever(),
                    monthlyRepeatProperty = MonthlyRepeatProperty(5)
                )

                FakeFactory.regularTransactionState.init(
                    listOf(
                        UserRegularTransaction(userId = user.id, transaction = regularTransaction1),
                        UserRegularTransaction(userId = user.id, transaction = regularTransaction2)
                    )
                )

                val result = bookletFeature.loadTransactionsForBookletForAMonth(
                    tokenValue,
                    105L,
                    java.time.Month.JANUARY,
                    2025
                )

                result.assertTrue { this.regularTransactions.size == 2 }
                result.assertTrue { this.regularTransactions.any { it.label == "Monthly Salary" } }
                result.assertTrue { this.regularTransactions.any { it.label == "Monthly Rent" } }
            }
        }

        @Test
        fun `Should return empty lists when booklet has no transactions`() {
            launchWithConnectedUserInstance {
                val booklet = Booklet(
                    amount = 500.toAmount(),
                    labelAccount = "Empty Account",
                    owner = user.toUser(),
                    id = 106L
                )

                accountState.init(listOf(AccountByOwner(listOf(booklet), user.id)))

                // Initialize regular transactions (empty list is valid)
                FakeFactory.regularTransactionState.init(emptyList())

                val result = bookletFeature.loadTransactionsForBookletForAMonth(
                    tokenValue,
                    106L,
                    java.time.Month.MAY,
                    2025
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
                        999L, // Non-existent booklet ID
                        java.time.Month.JUNE,
                        2025
                    )

                    result.assertFailure()
                }
            }

            @Test
            fun `Should only load transactions for the requested month and year`() {
                launchWithConnectedUserInstance {
                    val booklet = Booklet(
                        amount = 1000.toAmount(),
                        labelAccount = "Month Filter Test",
                        owner = user.toUser(),
                        id = 107L
                    )

                    accountState.init(listOf(AccountByOwner(listOf(booklet), user.id)))

                    // Add transactions in different months via the database
                    val transaction1 = Transaction(
                        id = 20L,
                        label = "January Transaction",
                        date = LocalDate.of(2025, 1, 15),
                        amount = 100.toAmount(),
                        isIncome = true,
                        isPreview = false
                    )
                    val transaction2 = Transaction(
                        id = 21L,
                        label = "February Transaction 1",
                        date = java.time.LocalDate.of(2025, 2, 10),
                        amount = 200.toAmount(),
                        isIncome = true,
                        isPreview = false
                    )
                    val transaction3 = Transaction(
                        id = 22L,
                        label = "February Transaction 2",
                        date = java.time.LocalDate.of(2025, 2, 20),
                        amount = 150.toAmount(),
                        isIncome = false,
                        isPreview = false
                    )
                    val transaction4 = Transaction(
                        id = 23L,
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
                                    IdUserAccount(user.id, 107L),
                                    mutableListOf(
                                        transaction1, transaction2, transaction3, transaction4
                                    )
                                )
                            )
                        )
                    // Initialize regular transactions (empty list is valid)
                    FakeFactory.regularTransactionState.init(emptyList())

                    val result = bookletFeature.loadTransactionsForBookletForAMonth(
                        tokenValue,
                        107L,
                        java.time.Month.FEBRUARY,
                        2025
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
                    val booklet = Booklet(
                        amount = 1000.toAmount(),
                        labelAccount = "Future Sold Test",
                        owner = user.toUser(),
                        id = 108L
                    )

                    accountState.init(listOf(AccountByOwner(listOf(booklet), user.id)))

                    // Add current month transaction and future preview transactions via the database
                    val transaction1 = Transaction(
                        id = 24L,
                        label = "Current Income",
                        date = java.time.LocalDate.of(2025, 7, 15),
                        amount = 500.toAmount(),
                        isIncome = true,
                        isPreview = false
                    )
                    val transaction2 = Transaction(
                        id = 25L,
                        label = "Future Income",
                        date = java.time.LocalDate.of(2025, 8, 10),
                        amount = 800.toAmount(),
                        isIncome = true,
                        isPreview = true
                    )
                    val transaction3 = Transaction(
                        id = 26L,
                        label = "Future Expense",
                        date = java.time.LocalDate.of(2025, 9, 5),
                        amount = 300.toAmount(),
                        isIncome = false,
                        isPreview = true
                    )

                    FakeFactory.fakeTransactionRepository()
                        .init(
                            listOf(
                                IdUserAccountByTransaction(
                                    IdUserAccount(user.id, 108L),
                                    mutableListOf(
                                        transaction1, transaction2, transaction3
                                    )
                                )
                            )
                        )
                    // Initialize regular transactions (empty list is valid)
                    FakeFactory.regularTransactionState.init(emptyList())

                    val result = bookletFeature.loadTransactionsForBookletForAMonth(
                        tokenValue,
                        108L,
                        java.time.Month.SEPTEMBER,
                        2025
                    )

                    // Real sold should only include current transactions
                    result.assertTrue { this.realSold == 1500.toAmount() } // 1000 + 500
                    // Previsional sold should include preview transactions up to September
                    result.assertTrue { this.previsionalSold.value > this.realSold.value }
                }
            }

            @Test
            fun `Should reflect add and remove of regular transaction via regularTransactionState`() {
                launchWithConnectedUserInstance {
                    val booklet = Booklet(
                        amount = 1000.toAmount(),
                        labelAccount = "RT CRUD Account",
                        owner = user.toUser(),
                        id = 200L
                    )
                    accountState.init(listOf(AccountByOwner(listOf(booklet), user.id)))

                    // start with empty regular transactions
                    FakeFactory.regularTransactionState.init(emptyList())

                    var result = bookletFeature.loadTransactionsForBookletForAMonth(
                        tokenValue, 200L, java.time.Month.JANUARY, 2025
                    )
                    // no regular transactions initially
                    result.assertTrue { this.regularTransactions.isEmpty() }

                    // create a regular transaction associated to this booklet
                    val regular = MonthlyTransaction(
                        label = "Monthly Income RT",
                        amount = 100.toAmount(),
                        isIncome = true,
                        id = RegularTransactionId("${user.id.value}-rt1"),
                        startDate = LocalDate.of(2025, 1, 1),
                        frequencyProperty = FrequencyProperty.Forever(),
                        monthlyRepeatProperty = MonthlyRepeatProperty(1)
                    )
                    FakeFactory.regularTransactionState.init(
                        listOf(
                            UserRegularTransaction(userId = user.id, transaction = regular)
                        )
                    )

                    result = bookletFeature.loadTransactionsForBookletForAMonth(
                        tokenValue, 200L, java.time.Month.JANUARY, 2025
                    )
                    result.assertTrue { this.regularTransactions.size == 1 }
                    result.assertTrue { this.regularTransactions.any { it.label == "Monthly Income RT" } }

                    // remove regular transactions
                    FakeFactory.regularTransactionState.init(emptyList())
                    result = bookletFeature.loadTransactionsForBookletForAMonth(
                        tokenValue, 200L, java.time.Month.JANUARY, 2025
                    )
                    result.assertTrue { this.regularTransactions.isEmpty() }
                }
            }

            @Test
            fun `Should not expose regular transactions of other user (multi-tenant isolation)`() {
                launchWithConnectedUserInstance {
                    val bookletMine = Booklet(1000.toAmount(), "My Account", owner = user.toUser(), id = 300L)
                    val otherUser = userRepository.register("other${UUID.randomUUID()}", "pw") as User
                    val bookletOther = Booklet(1000.toAmount(), "Other Account", owner = otherUser, id = 400L)

                    accountState.init(listOf(
                        AccountByOwner(listOf(bookletMine), user.id),
                        AccountByOwner(listOf(bookletOther), otherUser.id)
                    ))

                    // regular transactions: one for my user, one for other user
                    val rtMine = MonthlyTransaction(
                        label = "Mine RT",
                        amount = 50.toAmount(),
                        isIncome = true,
                        id = RegularTransactionId("${user.id.value}-mine"),
                        startDate = LocalDate.of(2025, 1, 1),
                        frequencyProperty = FrequencyProperty.Forever(),
                        monthlyRepeatProperty = MonthlyRepeatProperty(1)
                    )
                    val rtOther = MonthlyTransaction(
                        label = "Other RT",
                        amount = 999.toAmount(),
                        isIncome = true,
                        id = RegularTransactionId("${otherUser.id.value}-other"),
                        startDate = LocalDate.of(2025, 1, 1),
                        frequencyProperty = FrequencyProperty.Forever(),
                        monthlyRepeatProperty = MonthlyRepeatProperty(1)
                    )

                    FakeFactory.regularTransactionState.init(
                        listOf(
                            UserRegularTransaction(userId = user.id, transaction = rtMine),
                            UserRegularTransaction(userId = otherUser.id, transaction = rtOther)
                        )
                    )

                    val result = bookletFeature.loadTransactionsForBookletForAMonth(
                        tokenValue, 300L, java.time.Month.JANUARY, 2025
                    )

                    // assert only my regular transaction is returned
                    result.assertTrue { this.regularTransactions.size == 1 }
                    result.assertTrue { this.regularTransactions.any { it.label == "Mine RT" } }
                    result.assertTrue { this.regularTransactions.none { it.label == "Other RT" } }
                }
            }

            @Test
            fun `Should include regular transactions in previsional sold calculation`() {
                launchWithConnectedUserInstance {
                    val booklet = Booklet(
                        amount = 1000.toAmount(),
                        labelAccount = "RT Calculation Account",
                        owner = user.toUser(),
                        id = 301L
                    )
                    accountState.init(listOf(AccountByOwner(listOf(booklet), user.id)))

                    // Add a regular income transaction
                    val regularIncome = MonthlyTransaction(
                        label = "Monthly Salary",
                        amount = 2000.toAmount(),
                        isIncome = true,
                        id = RegularTransactionId("${user.id.value}-salary"),
                        startDate = LocalDate.of(2025, 1, 1),
                        frequencyProperty = FrequencyProperty.Forever(),
                        monthlyRepeatProperty = MonthlyRepeatProperty(1)
                    )

                    FakeFactory.regularTransactionState.init(
                        listOf(UserRegularTransaction(userId = user.id, transaction = regularIncome))
                    )

                    val result = bookletFeature.loadTransactionsForBookletForAMonth(
                        tokenValue, 301L, java.time.Month.JANUARY, 2025
                    )

                    // Previsional sold should include the regular transaction
                    result.assertTrue { this.regularTransactions.size == 1 }
                    result.assertTrue { this.previsionalSold.value >= BigDecimal(3000) }
                }
            }

            @Test
            fun `Should handle multiple regular transactions with different frequencies`() {
                launchWithConnectedUserInstance {
                    val booklet = Booklet(
                        amount = 1000.toAmount(),
                        labelAccount = "Multi RT Account",
                        owner = user.toUser(),
                        id = 302L
                    )
                    accountState.init(listOf(AccountByOwner(listOf(booklet), user.id)))

                    // Add multiple regular transactions with different start dates
                    val rt1 = MonthlyTransaction(
                        label = "RT Start Day 1",
                        amount = 500.toAmount(),
                        isIncome = true,
                        id = RegularTransactionId("${user.id.value}-rt1"),
                        startDate = LocalDate.of(2025, 1, 1),
                        frequencyProperty = FrequencyProperty.Forever(),
                        monthlyRepeatProperty = MonthlyRepeatProperty(1)
                    )
                    val rt2 = MonthlyTransaction(
                        label = "RT Start Day 15",
                        amount = 300.toAmount(),
                        isIncome = false,
                        id = RegularTransactionId("${user.id.value}-rt2"),
                        startDate = LocalDate.of(2025, 1, 15),
                        frequencyProperty = FrequencyProperty.Forever(),
                        monthlyRepeatProperty = MonthlyRepeatProperty(15)
                    )
                    val rt3 = MonthlyTransaction(
                        label = "RT Start Day 25",
                        amount = 200.toAmount(),
                        isIncome = true,
                        id = RegularTransactionId("${user.id.value}-rt3"),
                        startDate = LocalDate.of(2025, 1, 25),
                        frequencyProperty = FrequencyProperty.Forever(),
                        monthlyRepeatProperty = MonthlyRepeatProperty(25)
                    )

                    FakeFactory.regularTransactionState.init(
                        listOf(
                            UserRegularTransaction(userId = user.id, transaction = rt1),
                            UserRegularTransaction(userId = user.id, transaction = rt2),
                            UserRegularTransaction(userId = user.id, transaction = rt3)
                        )
                    )

                    val result = bookletFeature.loadTransactionsForBookletForAMonth(
                        tokenValue, 302L, java.time.Month.JANUARY, 2025
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
                    val booklet = Booklet(
                        amount = 1000.toAmount(),
                        labelAccount = "Combined Account",
                        owner = user.toUser(),
                        id = 303L
                    )
                    accountState.init(listOf(AccountByOwner(listOf(booklet), user.id)))

                    // Add a current transaction
                    val currentTx = Transaction(
                        id = 100L,
                        label = "Current Expense",
                        date = LocalDate.of(2025, 6, 10),
                        amount = 100.toAmount(),
                        isIncome = false,
                        isPreview = false
                    )
                    // Add a preview transaction
                    val previewTx = Transaction(
                        id = 101L,
                        label = "Preview Income",
                        date = LocalDate.of(2025, 6, 20),
                        amount = 500.toAmount(),
                        isIncome = true,
                        isPreview = true
                    )

                    FakeFactory.fakeTransactionRepository().init(
                        listOf(
                            IdUserAccountByTransaction(
                                IdUserAccount(user.id, 303L),
                                mutableListOf(currentTx, previewTx)
                            )
                        )
                    )

                    // Add a regular transaction
                    val regularTx = MonthlyTransaction(
                        label = "Regular Income",
                        amount = 2000.toAmount(),
                        isIncome = true,
                        id = RegularTransactionId("${user.id.value}-regular"),
                        startDate = LocalDate.of(2025, 6, 1),
                        frequencyProperty = FrequencyProperty.Forever(),
                        monthlyRepeatProperty = MonthlyRepeatProperty(1)
                    )

                    FakeFactory.regularTransactionState.init(
                        listOf(UserRegularTransaction(userId = user.id, transaction = regularTx))
                    )

                    val result = bookletFeature.loadTransactionsForBookletForAMonth(
                        tokenValue, 303L, java.time.Month.JUNE, 2025
                    )

                    result.assertTrue { this.currentTransactions.size == 1 }
                    result.assertTrue { this.previsionalTransactions.size == 1 }
                    result.assertTrue { this.regularTransactions.size == 1 }
                    result.assertTrue { this.realSold == 900.toAmount() } // 1000 - 100
                    result.assertTrue { this.previsionalSold.value > this.realSold.value }
                }
            }

            @Test
            fun `Should not include regular transactions that started after the requested month`() {
                launchWithConnectedUserInstance {
                    val booklet = Booklet(
                        amount = 1000.toAmount(),
                        labelAccount = "Future RT Account",
                        owner = user.toUser(),
                        id = 304L
                    )
                    accountState.init(listOf(AccountByOwner(listOf(booklet), user.id)))

                    // Regular transaction starting in March
                    val futureRT = MonthlyTransaction(
                        label = "Future RT",
                        amount = 500.toAmount(),
                        isIncome = true,
                        id = RegularTransactionId("${user.id.value}-future"),
                        startDate = LocalDate.of(2025, 3, 1),
                        frequencyProperty = FrequencyProperty.Forever(),
                        monthlyRepeatProperty = MonthlyRepeatProperty(1)
                    )

                    FakeFactory.regularTransactionState.init(
                        listOf(UserRegularTransaction(userId = user.id, transaction = futureRT))
                    )

                    val result = bookletFeature.loadTransactionsForBookletForAMonth(
                        tokenValue, 304L, java.time.Month.JANUARY, 2025
                    )

                    result.assertTrue { this.regularTransactions.isEmpty() }
                }
            }
        }
    }
}