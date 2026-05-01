package fr.sacane.jmanager.domain.port

import fr.sacane.jmanager.domain.*
import fr.sacane.jmanager.domain.fake.BookletsByOwner
import fr.sacane.jmanager.domain.fake.FakeFactory
import fr.sacane.jmanager.domain.fake.IdUserBooklet
import fr.sacane.jmanager.domain.fake.IdBookletByTransaction
import fr.sacane.jmanager.domain.fake.UserRegularTransaction
import fr.sacane.jmanager.domain.models.*
import fr.sacane.jmanager.domain.models.transaction.Transaction
import fr.sacane.jmanager.domain.models.transaction.regular.FrequencyProperty
import fr.sacane.jmanager.domain.models.transaction.regular.RecurrenceRule
import fr.sacane.jmanager.domain.models.transaction.regular.RegularTransaction
import fr.sacane.jmanager.domain.models.transaction.regular.RegularTransactionId
import fr.sacane.jmanager.domain.port.input.booklet.*
import fr.sacane.jmanager.domain.port.output.UserRepository
import fr.sacane.jmanager.domain.utils.Result
import fr.sacane.jmanager.domain.utils.ResultState
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.LocalDate
import java.time.YearMonth
import java.util.*
import kotlin.collections.emptyList

class BookletFeatureTest: FeatureTest() {

    companion object{
        private val userRepository: UserRepository = FakeFactory.fakeUserRepository()
        private val findBookletByIdService = FakeFactory.findBookletByIdService
        private val editBookletService = FakeFactory.editBookletService
        private val deleteBookletByIdService = FakeFactory.deleteBookletByIdService
        private val findByLabelAndUserIdService = FakeFactory.findByLabelAndUserIdService
        private val findAllRegisteredBookletsService = FakeFactory.findAllRegisteredBookletsService
        private val saveBookletService = FakeFactory.saveBookletService
        private val loadTransactionsForBookletForAMonthService = FakeFactory.loadTransactionsForBookletForAMonthService
        private val loadBalancesForBookletForAMonthService = FakeFactory.loadBalancesForBookletForAMonthService
        private val regenerateDeletedPrevisionalTransactionsService = FakeFactory.regenerateDeletedPrevisionalTransactionsService
        private val user = userRepository.register("jojo", "test") as User
        private val bookletState: State<BookletsByOwner> = FakeFactory.bookletState()
    }

    @AfterEach
    fun clear(){
        FakeFactory.clearAll()
    }

    @Test
    fun `Should find booklet by its Id`() {
        val id = UUID.randomUUID()
        val element = Booklet(Amount.fromString("100", "€".asCurrency()), "test", owner = user, id= id)
        bookletState.init(listOf(
            BookletsByOwner(listOf(element), user.id)
        ))
        findBookletByIdService.handle(FindBookletByIdQuery(id, user.id))
            .assertTrue {
                this.label == "test"
            }
    }

    @Test
    fun `Given an existing booklet it could be edit`() {
        val element = Booklet(Amount.fromString("100", "€".asCurrency()), "test", owner = user, id = UUID.randomUUID())
        bookletState.init(listOf(
            BookletsByOwner(listOf(element), user.id)
        ))
        val booklet = Booklet(
            Amount(BigDecimal(102)),
            label = element.label,
            initialSold = element.initialSold,
            owner = user,
            id= element.id,
        )
        val response = editBookletService.handle(EditBookletCommand(booklet, user.id))

        val expectedAnswer = Amount(BigDecimal(102))

        response.map { it.amount }.assertEquals(expectedAnswer)
    }

    @Test
    fun `As an owner of an booklet, I can delete it`() {
        val element = Booklet( Amount.fromString("100", "€".asCurrency()), "test", owner = user, id = UUID.randomUUID())
        bookletState.init(listOf(
            BookletsByOwner(listOf(element), user.id)
        ))

        deleteBookletByIdService.handle(DeleteBookletByIdCommand(element.id!!, user.id)).assertTrue {
            val bookletsList = bookletState.getStates()

            val expectedBookletSize = 0
            val actualBookletSize = bookletsList.find { it.userId == user.id }?.booklets?.size ?: throw Error()
            expectedBookletSize == actualBookletSize
        }

        val bookletsList = bookletState.getStates()
        val ofUser = bookletsList.find { it.userId == user.id }!!

        assertNull(ofUser.existsById(UUID.randomUUID()))
    }

    @Test
    fun `As an booklet's owner, I can retrieve it by its label`() {
        launchWithConnectedUserInstance {
            val element = Booklet(Amount.fromString("100", "€".asCurrency()), "test22", owner = Companion.user, id = UUID.randomUUID())
            bookletState.init(listOf(
                BookletsByOwner(listOf(element), this.user.id)
            ))
            findByLabelAndUserIdService.handle(FindByLabelAndUserIdQuery(user.id, element.label))
                .assertTrue {
                    this.label == "test22" && this.amount == Amount(100)
                }
        }
    }

    @Test
    fun `As a booklet's owner,  I can retrieve All of my Registered Booklets`() {
        launchWithConnectedUserWithoutBooklet {

            val booklet = Booklet(Amount.fromString("100", "€".asCurrency()), "test1", owner = user, id = UUID.randomUUID())
            val booklet2 = Booklet(Amount.fromString("100", "€".asCurrency()), "test2", owner = user, id = UUID.randomUUID())
            val booklet3 = Booklet( Amount.fromString("100", "€".asCurrency()), "test3", owner = user, id= UUID.randomUUID())
            val booklet4 = Booklet( Amount.fromString("100", "€".asCurrency()), "test4", owner = user, id = UUID.randomUUID())
            val expectedBookletList = listOf(
                booklet,
                booklet2,
                booklet3,
                booklet4
            )
            bookletState.init(listOf(
                BookletsByOwner(expectedBookletList, userId)
            ))

            findAllRegisteredBookletsService.handle(FindAllRegisteredBookletsQuery(userId))
                .assertContainsAtPosition(0, booklet)
                .assertContainsAtPosition(1, booklet2)
                .assertContainsAtPosition(2, booklet3)
                .assertContainsAtPosition(3, booklet4)
        }

    }

    @Test
    fun `As a Jmanager user, I can create new booklet`() {
        launchWithConnectedUserWithoutBooklet {
            val bookletToSave = Booklet( Amount.fromString("100", "€".asCurrency()), "test1", owner = user, id = UUID.randomUUID())

            saveBookletService.handle(SaveBookletCommand(userId, bookletToSave))
                .assertTrue {
                    val expectedAmount = Amount(100)
                    val expectedLabel = "test1"
                    this.amount == expectedAmount && this.label == expectedLabel
                }
        }
    }

    @Test
    fun `As a simple user, I cannot create more than six booklets`() {
        launchWithConnectedUserWithoutBooklet {
            val bookletLists = mutableListOf<Booklet>()
            repeat(6) {
                val bookletToSave = Booklet( Amount.fromString("100", "€".asCurrency()), "test$it", owner = user, id = UUID.randomUUID())
                bookletLists.add(bookletToSave)
            }
            bookletState.init(listOf(
                BookletsByOwner(bookletLists, userId)
            ))

            val result = saveBookletService.handle(SaveBookletCommand(
                userId,
                Booklet( Amount.fromString("100", "€".asCurrency()), "test7", owner = user, id = UUID.randomUUID())
            ))

            result.assertFailure(ResultState.BOOKLET_MAXIMUM_SIZE_REACHED)
            assertEquals("domain.booklet.save.maximum_size_reached", result.errorInfo?.key)
        }
    }

    @Test
    fun `As a booklet's owner, I cannot register an existing booklet with the same label`() {
        val testUser = userRepository.register("testBookletOwner", "pass") as User
        bookletState.init(listOf(
            BookletsByOwner(listOf(Booklet(Amount.fromString("100", "€".asCurrency()), "test1", owner = testUser, id = UUID.randomUUID())), testUser.id)
        ))

        val bookletToSave = Booklet( Amount.fromString("150", "€".asCurrency()), "test1", owner = testUser, id = UUID.randomUUID())
        saveBookletService.handle(SaveBookletCommand(testUser.id, bookletToSave))
            .assertFailure(ResultState.BOOKLET_LABEL_EXIST)
    }

    @Test
    fun `Deleting a booklet owned by another user should be forbidden`() {
        val victimUser = userRepository.register("victim", "pass") as User
        val victimBooklet = Booklet(Amount.fromString("500", "€".asCurrency()), "victim-booklet", owner = victimUser, id = UUID.randomUUID())
        bookletState.init(listOf(
            BookletsByOwner(listOf(victimBooklet), victimUser.id)
        ))

        val result = deleteBookletByIdService.handle(DeleteBookletByIdCommand(victimBooklet.id!!, user.id))
        result.assertFailure(ResultState.FORBIDDEN)
    }

    @Test
    fun `Editing a booklet owned by another user should be forbidden`() {
        val victimUser = userRepository.register("victim2", "pass") as User
        val victimBooklet = Booklet(Amount.fromString("500", "€".asCurrency()), "victim-booklet2", owner = victimUser, id = UUID.randomUUID())
        bookletState.init(listOf(
            BookletsByOwner(listOf(victimBooklet), victimUser.id)
        ))

        val editedBooklet = Booklet(Amount.fromString("999", "€".asCurrency()), "hacked", owner = victimUser, id = victimBooklet.id)
        val result = editBookletService.handle(EditBookletCommand(editedBooklet, user.id))
        result.assertFailure(ResultState.FORBIDDEN)
    }

    @Nested
    inner class LoadTransactionsForBookletForAMonthTest {

        @Test
        fun `Should calculate real sold correctly with income transactions only`() {
            launchWithConnectedUserInstance {
                val bookletId = UUID.randomUUID()
                val booklet = Booklet(
                    amount = 1000.toAmount(),
                    label = "Test Booklet",
                    owner = user.toUser(),
                    id = bookletId
                )

                bookletState.init(listOf(BookletsByOwner(listOf(booklet), user.id)))

                val transaction1 = Transaction(
                    id = UUID.randomUUID(),
                    label = "Salary",
                    date = LocalDate.of(2025, 1, 15),
                    amount = 2000.toAmount(),
                    isIncome = true,
                    isPreview = false
                )
                val transaction2 = Transaction(
                    id = UUID.randomUUID(),
                    label = "Bonus",
                    date = LocalDate.of(2025, 1, 20),
                    amount = 500.toAmount(),
                    isIncome = true,
                    isPreview = false
                )
                FakeFactory.fakeTransactionRepository()
                    .init(listOf(IdBookletByTransaction(IdUserBooklet(user.id, bookletId), mutableListOf(transaction1))))
                FakeFactory.fakeTransactionRepository()
                    .init(listOf(IdBookletByTransaction(IdUserBooklet(user.id, bookletId), mutableListOf(transaction2))))


                FakeFactory.regularTransactionState.init(emptyList())

                val result = loadTransactionsForBookletForAMonthService.handle(LoadTransactionsForBookletForAMonthQuery(
                    user.id,
                    bookletId,
                    java.time.Month.JANUARY,
                    2025,
                    startingMonth = java.time.Month.JANUARY,
                    startingYear = 2025
                ))

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
                    label = "Test Booklet",
                    owner = user.toUser(),
                    id = bookletId
                )

                bookletState.init(listOf(BookletsByOwner(listOf(booklet), user.id)))

                val transaction1 = Transaction(
                    id = UUID.randomUUID(),
                    label = "Rent",
                    date = LocalDate.of(2025, 1, 5),
                    amount = 500.toAmount(),
                    isIncome = false,
                    isPreview = false
                )
                val transaction2 = Transaction(
                    id = UUID.randomUUID(),
                    label = "Groceries",
                    date = LocalDate.of(2025, 1, 10),
                    amount = 200.toAmount(),
                    isIncome = false,
                    isPreview = false
                )
                FakeFactory.fakeTransactionRepository()
                    .init(
                        listOf(
                            IdBookletByTransaction(
                                IdUserBooklet(user.id, bookletId),
                                mutableListOf(transaction1, transaction2)
                            )
                        )
                    )

                FakeFactory.regularTransactionState.init(emptyList())

                val result = loadTransactionsForBookletForAMonthService.handle(LoadTransactionsForBookletForAMonthQuery(
                    user.id,
                    bookletId,
                    java.time.Month.JANUARY,
                    2025,
                    startingMonth = java.time.Month.JANUARY,
                    startingYear = 2025
                ))

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
                    label = "Mixed Booklet",
                    owner = user.toUser(),
                    id = bookletId
                )

                bookletState.init(listOf(BookletsByOwner(listOf(booklet), user.id)))

                val transaction1 = Transaction(
                    id = UUID.randomUUID(),
                    label = "Income 1",
                    date = LocalDate.of(2025, 2, 5),
                    amount = 1500.toAmount(),
                    isIncome = true,
                    isPreview = false
                )
                val transaction2 = Transaction(
                    id = UUID.randomUUID(),
                    label = "Expense 1",
                    date = LocalDate.of(2025, 2, 10),
                    amount = 300.toAmount(),
                    isIncome = false,
                    isPreview = false
                )
                val transaction3 = Transaction(
                    id = UUID.randomUUID(),
                    label = "Income 2",
                    date = LocalDate.of(2025, 2, 15),
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
                            IdBookletByTransaction(
                                IdUserBooklet(user.id, bookletId),
                                mutableListOf(
                                    transaction1, transaction2, transaction3, transaction4
                                )
                            )
                        )
                    )
                FakeFactory.regularTransactionState.init(emptyList())

                val result = loadTransactionsForBookletForAMonthService.handle(LoadTransactionsForBookletForAMonthQuery(
                    user.id,
                    bookletId,
                    java.time.Month.FEBRUARY,
                    2025,
                    startingMonth = java.time.Month.FEBRUARY,
                    startingYear = 2025
                ))
                result.assertTrue { this.realSold == 2500.toAmount() } // 1000 + 1500 - 300 + 800 - 500 = 2500
            }
        }

        @Test
        fun `Should calculate previsional sold correctly with preview transactions`() {
            val bookletId = UUID.randomUUID()
            launchWithConnectedUserInstance {
                val booklet = Booklet(
                    amount = 1000.toAmount(),
                    label = "Preview Booklet",
                    owner = user.toUser(),
                    id = bookletId
                )

                bookletState.init(listOf(BookletsByOwner(listOf(booklet), user.id)))

                val transaction1 = Transaction(
                    id = UUID.randomUUID(),
                    label = "Current Income",
                    date = LocalDate.of(2025, 11, 5),
                    amount = 500.toAmount(),
                    isIncome = true,
                    isPreview = false
                )
                val transaction2 = Transaction(
                    id = UUID.randomUUID(),
                    label = "Future Income",
                    date = LocalDate.of(2025, 11, 25),
                    amount = 1000.toAmount(),
                    isIncome = true,
                    isPreview = true
                )
                val transaction3 = Transaction(
                    id = UUID.randomUUID(),
                    label = "Future Expense",
                    date = LocalDate.of(2025, 11, 28),
                    amount = 300.toAmount(),
                    isIncome = false,
                    isPreview = true
                )
                FakeFactory.fakeTransactionRepository()
                    .init(
                        listOf(
                            IdBookletByTransaction(
                                IdUserBooklet(user.id, bookletId),
                                mutableListOf(transaction1, transaction2, transaction3)
                            )
                        )
                    )
                FakeFactory.regularTransactionState.init(emptyList())

                val result = loadTransactionsForBookletForAMonthService.handle(LoadTransactionsForBookletForAMonthQuery(
                    user.id,
                    bookletId,
                    java.time.Month.NOVEMBER,
                    2025,
                    startingMonth = java.time.Month.NOVEMBER,
                    startingYear = 2025
                ))

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
                    label = "Separation Test",
                    owner = user.toUser(),
                    id = bookletId
                )

                bookletState.init(listOf(BookletsByOwner(listOf(booklet), user.id)))

                val transaction1 = Transaction(
                    id = UUID.randomUUID(),
                    label = "Current 1",
                    date = LocalDate.of(2025, 4, 1),
                    amount = 100.toAmount(),
                    isIncome = true,
                    isPreview = false
                )
                val transaction2 = Transaction(
                    id = UUID.randomUUID(),
                    label = "Current 2",
                    date = LocalDate.of(2025, 4, 10),
                    amount = 50.toAmount(),
                    isIncome = false,
                    isPreview = false
                )
                val transaction3 = Transaction(
                    id = UUID.randomUUID(),
                    label = "Current 3",
                    date = LocalDate.of(2025, 4, 15),
                    amount = 75.toAmount(),
                    isIncome = true,
                    isPreview = false
                )
                val transaction4 = Transaction(
                    id = UUID.randomUUID(),
                    label = "Preview 1",
                    date = LocalDate.of(2025, 4, 20),
                    amount = 200.toAmount(),
                    isIncome = true,
                    isPreview = true
                )
                val transaction5 = Transaction(
                    id = UUID.randomUUID(),
                    label = "Preview 2",
                    date = LocalDate.of(2025, 4, 25),
                    amount = 150.toAmount(),
                    isIncome = false,
                    isPreview = true
                )

                FakeFactory.fakeTransactionRepository()
                    .init(
                        listOf(
                            IdBookletByTransaction(
                                IdUserBooklet(user.id, bookletId),
                                mutableListOf(
                                    transaction1, transaction2, transaction3, transaction4, transaction5
                                )
                            )
                        )
                    )
                FakeFactory.regularTransactionState.init(emptyList())

                val result = loadTransactionsForBookletForAMonthService.handle(LoadTransactionsForBookletForAMonthQuery(
                    user.id,
                    bookletId,
                    java.time.Month.APRIL,
                    2025,
                    startingMonth = java.time.Month.APRIL,
                    startingYear = 2025
                ))

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
                    label = "Regular Booklet",
                    owner = user.toUser(),
                    id = bookletId
                )

                bookletState.init(listOf(BookletsByOwner(listOf(booklet), user.id)))

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

                val result = loadTransactionsForBookletForAMonthService.handle(LoadTransactionsForBookletForAMonthQuery(
                    user.id,
                    bookletId,
                    java.time.Month.JANUARY,
                    2025,
                    startingMonth = java.time.Month.JANUARY,
                    startingYear = 2025
                ))

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
                    label = "Empty Booklet",
                    owner = user.toUser(),
                    id = bookletId
                )

                bookletState.init(listOf(BookletsByOwner(listOf(booklet), user.id)))

                FakeFactory.regularTransactionState.init(emptyList())

                val result = loadTransactionsForBookletForAMonthService.handle(LoadTransactionsForBookletForAMonthQuery(
                    user.id,
                    bookletId,
                    java.time.Month.MAY,
                    2025,
                    startingMonth = java.time.Month.MAY,
                    startingYear = 2025
                ))

                result.assertTrue { this.currentTransactions.isEmpty() }
                result.assertTrue { this.previsionalTransactions.isEmpty() }
                result.assertTrue { this.realSold == 500.toAmount() }
                result.assertTrue { this.label == "Empty Booklet" }
            }
        }

        @Test
        fun `Should fail when booklet does not exist`() {
            launchWithConnectedUserInstance {
                val result = loadTransactionsForBookletForAMonthService.handle(LoadTransactionsForBookletForAMonthQuery(
                    user.id,
                    UUID.randomUUID(), // Non-existent booklet ID
                    java.time.Month.JUNE,
                    2025,
                    startingMonth = java.time.Month.JUNE,
                    startingYear = 2025
                ))

                result.assertFailure()
            }
        }

        @Test
        fun `Should only load transactions for the requested month and year`() {
            launchWithConnectedUserInstance {
                val bookletId = UUID.randomUUID()
                val booklet = Booklet(
                    amount = 1000.toAmount(),
                    label = "Month Filter Test",
                    owner = user.toUser(),
                    id = bookletId
                )

                bookletState.init(listOf(BookletsByOwner(listOf(booklet), user.id)))

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
                    date = LocalDate.of(2025, 2, 20),
                    amount = 150.toAmount(),
                    isIncome = false,
                    isPreview = false
                )
                val transaction4 = Transaction(
                    id = UUID.randomUUID(),
                    label = "March Transaction",
                    date = LocalDate.of(2025, 3, 5),
                    amount = 50.toAmount(),
                    isIncome = true,
                    isPreview = false
                )

                FakeFactory.fakeTransactionRepository()
                    .init(
                        listOf(
                            IdBookletByTransaction(
                                IdUserBooklet(user.id, bookletId),
                                mutableListOf(
                                    transaction1, transaction2, transaction3, transaction4
                                )
                            )
                        )
                    )
                FakeFactory.regularTransactionState.init(emptyList())

                val result = loadTransactionsForBookletForAMonthService.handle(LoadTransactionsForBookletForAMonthQuery(
                    user.id,
                    bookletId,
                    java.time.Month.FEBRUARY,
                    2025,
                    startingMonth = java.time.Month.FEBRUARY,
                    startingYear = 2025
                ))

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
                    label = "Future Sold Test",
                    owner = user.toUser(),
                    id = bookletId
                )

                bookletState.init(listOf(BookletsByOwner(listOf(booklet), user.id)))

                val transaction1 = Transaction(
                    id = UUID.randomUUID(),
                    label = "Current Income",
                    date = LocalDate.of(2025, 11, 15),
                    amount = 500.toAmount(),
                    isIncome = true,
                    isPreview = false
                )
                val transaction2 = Transaction(
                    id = UUID.randomUUID(),
                    label = "Future Income",
                    date = LocalDate.of(2025, 12, 10),
                    amount = 800.toAmount(),
                    isIncome = true,
                    isPreview = true
                )
                val transaction3 = Transaction(
                    id = UUID.randomUUID(),
                    label = "Future Expense",
                    date = LocalDate.of(2026, 1, 5),
                    amount = 300.toAmount(),
                    isIncome = false,
                    isPreview = true
                )

                FakeFactory.fakeTransactionRepository()
                    .init(
                        listOf(
                            IdBookletByTransaction(
                                IdUserBooklet(user.id, bookletId),
                                mutableListOf(
                                    transaction1, transaction2, transaction3
                                )
                            )
                        )
                    )
                FakeFactory.regularTransactionState.init(emptyList())

                val result = loadTransactionsForBookletForAMonthService.handle(LoadTransactionsForBookletForAMonthQuery(
                    user.id,
                    bookletId,
                    java.time.Month.JANUARY,
                    2026,
                    startingMonth = java.time.Month.NOVEMBER,
                    startingYear = 2025
                ))

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
                    label = "RT CRUD Booklet",
                    owner = user.toUser(),
                    id = bookletId
                )
                bookletState.init(listOf(BookletsByOwner(listOf(booklet), user.id)))

                FakeFactory.regularTransactionState.init(emptyList())

                var result = loadTransactionsForBookletForAMonthService.handle(LoadTransactionsForBookletForAMonthQuery(
                    user.id, bookletId, java.time.Month.JANUARY, 2025,
                    startingMonth = java.time.Month.JANUARY,
                    startingYear = 2025
                ))
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

                result = loadTransactionsForBookletForAMonthService.handle(LoadTransactionsForBookletForAMonthQuery(
                    user.id, bookletId, java.time.Month.JANUARY, 2025,
                    startingMonth = java.time.Month.JANUARY,
                    startingYear = 2025
                ))
                result.assertTrue { this.regularTransactions.size == 1 }
                result.assertTrue { this.regularTransactions.any { it.label == "Monthly Income RT" } }

                FakeFactory.regularTransactionState.init(emptyList())
                result = loadTransactionsForBookletForAMonthService.handle(LoadTransactionsForBookletForAMonthQuery(
                    user.id, bookletId, java.time.Month.JANUARY, 2025,
                    startingMonth = java.time.Month.JANUARY,
                    startingYear = 2025
                ))
                result.assertTrue { this.regularTransactions.isEmpty() }
            }
        }

        @Test
        fun `Should not expose regular transactions of other user (multi-tenant isolation)`() {
            launchWithConnectedUserInstance {
                val bookletIdMine = UUID.randomUUID()
                val bookletMine = Booklet(1000.toAmount(), "My Booklet", owner = user.toUser(), id = bookletIdMine)
                val otherUser = userRepository.register("other${UUID.randomUUID()}", "pw") as User
                val bookletIdOther = UUID.randomUUID()
                val bookletOther = Booklet(1000.toAmount(), "Other Booklet", owner = otherUser, id = bookletIdOther)

                bookletState.init(listOf(
                    BookletsByOwner(listOf(bookletMine), user.id),
                    BookletsByOwner(listOf(bookletOther), otherUser.id)
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

                val result = loadTransactionsForBookletForAMonthService.handle(LoadTransactionsForBookletForAMonthQuery(
                    user.id, bookletIdMine, java.time.Month.JANUARY, 2025,
                    startingMonth = java.time.Month.JANUARY,
                    startingYear = 2025
                ))

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
                    label = "RT Calculation Booklet",
                    owner = user.toUser(),
                    id = bookletId
                )
                bookletState.init(listOf(BookletsByOwner(listOf(booklet), user.id)))

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

                val result = loadTransactionsForBookletForAMonthService.handle(LoadTransactionsForBookletForAMonthQuery(
                    user.id, bookletId, java.time.Month.NOVEMBER, 2025,
                    startingMonth = java.time.Month.NOVEMBER,
                    startingYear = 2025
                ))
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
                    label = "Multi RT Booklet",
                    owner = user.toUser(),
                    id = bookletId
                )
                bookletState.init(listOf(BookletsByOwner(listOf(booklet), user.id)))

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

                val result = loadTransactionsForBookletForAMonthService.handle(LoadTransactionsForBookletForAMonthQuery(
                    user.id, bookletId, java.time.Month.JANUARY, 2025,
                    startingMonth = java.time.Month.JANUARY,
                    startingYear = 2025
                ))

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
                    label = "Combined Booklet",
                    owner = user.toUser(),
                    id = bookletId
                )
                bookletState.init(listOf(BookletsByOwner(listOf(booklet), user.id)))

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
                        IdBookletByTransaction(
                            IdUserBooklet(user.id, bookletId),
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

                val result = loadTransactionsForBookletForAMonthService.handle(LoadTransactionsForBookletForAMonthQuery(
                    user.id, bookletId, java.time.Month.DECEMBER, 2025,
                    startingMonth = java.time.Month.DECEMBER,
                    startingYear = 2025
                ))

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
                    label = "Future RT Booklet",
                    owner = user.toUser(),
                    id = bookletId
                )
                bookletState.init(listOf(BookletsByOwner(listOf(booklet), user.id)))

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

                val result = loadTransactionsForBookletForAMonthService.handle(LoadTransactionsForBookletForAMonthQuery(
                    user.id, bookletId, java.time.Month.JANUARY, 2025
                ))

                result.assertTrue { this.regularTransactions.isEmpty() }
            }
        }

        @Test
        fun `Should not double count virtual regular transactions when months already have confirmed physical transactions`() {
            launchWithConnectedUserInstance {
                val bookletId = UUID.randomUUID()
                val booklet = Booklet(
                    amount = 2000.toAmount(),
                    label = "No Double Count",
                    owner = user.toUser(),
                    id = bookletId
                )
                bookletState.init(listOf(BookletsByOwner(listOf(booklet), user.id)))

                val regularTransactionId = RegularTransactionId("${user.id.value}-salary")
                val regularIncome = RegularTransaction(
                    label = "Monthly Salary",
                    amount = 500.toAmount(),
                    isIncome = true,
                    id = regularTransactionId,
                    startDate = LocalDate.of(2026, 1, 5),
                    frequencyProperty = FrequencyProperty.Forever(),
                    recurrenceRule = RecurrenceRule.Monthly(5)
                )

                FakeFactory.regularTransactionState.init(
                    listOf(UserRegularTransaction(userId = user.id, transaction = regularIncome, bookletIds = listOf(bookletId)))
                )

                val febConfirmed = Transaction(
                    id = UUID.randomUUID(),
                    label = "Monthly Salary",
                    date = LocalDate.of(2026, 2, 5),
                    amount = 500.toAmount(),
                    isIncome = true,
                    isPreview = false,
                    regularTransactionId = regularTransactionId
                )
                val marConfirmed = Transaction(
                    id = UUID.randomUUID(),
                    label = "Monthly Salary",
                    date = LocalDate.of(2026, 3, 5),
                    amount = 500.toAmount(),
                    isIncome = true,
                    isPreview = false,
                    regularTransactionId = regularTransactionId
                )

                FakeFactory.fakeTransactionRepository().init(
                    listOf(
                        IdBookletByTransaction(
                            IdUserBooklet(user.id, bookletId),
                            mutableListOf(febConfirmed, marConfirmed)
                        )
                    )
                )

                val result = loadTransactionsForBookletForAMonthService.handle(LoadTransactionsForBookletForAMonthQuery(
                    user.id,
                    bookletId,
                    java.time.Month.MARCH,
                    2026,
                    startingMonth = java.time.Month.FEBRUARY,
                    startingYear = 2026
                ))

                result.assertTrue { this.previsionalSold == this.realSold }
            }
        }

        @Test
        fun `Balances endpoint should not double count virtual regular transactions when confirmed physical already exists`() {
            launchWithConnectedUserInstance {
                val bookletId = UUID.randomUUID()
                val booklet = Booklet(
                    amount = 2000.toAmount(),
                    label = "No Double Count Balances",
                    owner = user.toUser(),
                    id = bookletId
                )
                bookletState.init(listOf(BookletsByOwner(listOf(booklet), user.id)))

                val regularTransactionId = RegularTransactionId("${user.id.value}-salary-balances")
                val regularIncome = RegularTransaction(
                    label = "Monthly Salary",
                    amount = 500.toAmount(),
                    isIncome = true,
                    id = regularTransactionId,
                    startDate = LocalDate.of(2026, 1, 5),
                    frequencyProperty = FrequencyProperty.Forever(),
                    recurrenceRule = RecurrenceRule.Monthly(5)
                )

                FakeFactory.regularTransactionState.init(
                    listOf(UserRegularTransaction(userId = user.id, transaction = regularIncome, bookletIds = listOf(bookletId)))
                )

                val marConfirmed = Transaction(
                    id = UUID.randomUUID(),
                    label = "Monthly Salary",
                    date = LocalDate.of(2026, 3, 5),
                    amount = 500.toAmount(),
                    isIncome = true,
                    isPreview = false,
                    regularTransactionId = regularTransactionId
                )

                FakeFactory.fakeTransactionRepository().init(
                    listOf(
                        IdBookletByTransaction(
                            IdUserBooklet(user.id, bookletId),
                            mutableListOf(marConfirmed)
                        )
                    )
                )

                val result = loadBalancesForBookletForAMonthService.handle(LoadBalancesForBookletForAMonthQuery(
                    user.id,
                    bookletId,
                    java.time.Month.MARCH,
                    2026,
                    startingMonth = java.time.Month.MARCH,
                    startingYear = 2026
                ))

                result.assertTrue { this.previewSold == this.realSold }
            }
        }

        @Test
        fun `Balances endpoint should include preview transactions in current-to-target month range`() {
            launchWithConnectedUserInstance {
                val bookletId = UUID.randomUUID()
                val booklet = Booklet(
                    amount = 1000.toAmount(),
                    label = "Range Coverage",
                    owner = user.toUser(),
                    id = bookletId
                )
                bookletState.init(listOf(BookletsByOwner(listOf(booklet), user.id)))

                val febPreviewIncome = Transaction(
                    id = UUID.randomUUID(),
                    label = "Projected income",
                    date = LocalDate.of(2026, 2, 10),
                    amount = 100.toAmount(),
                    isIncome = true,
                    isPreview = true
                )

                FakeFactory.fakeTransactionRepository().init(
                    listOf(
                        IdBookletByTransaction(
                            IdUserBooklet(user.id, bookletId),
                            mutableListOf(febPreviewIncome)
                        )
                    )
                )

                FakeFactory.regularTransactionState.init(emptyList())

                val result = loadBalancesForBookletForAMonthService.handle(LoadBalancesForBookletForAMonthQuery(
                    user.id,
                    bookletId,
                    java.time.Month.MARCH,
                    2026,
                    startingMonth = java.time.Month.JANUARY,
                    startingYear = 2026
                ))

                result.assertTrue { this.realSold == 1000.toAmount() }
                result.assertTrue { this.previewSold == 1100.toAmount() }
            }
        }

        @Test
        fun `Should not expose null id previsional transactions for current month`() {
            launchWithConnectedUserInstance {
                val bookletId = UUID.randomUUID()
                val booklet = Booklet(
                    amount = 1000.toAmount(),
                    label = "Current Month No Null Id",
                    owner = user.toUser(),
                    id = bookletId
                )
                bookletState.init(listOf(BookletsByOwner(listOf(booklet), user.id)))

                val regularTx = RegularTransaction(
                    label = "Current month regular",
                    amount = 120.toAmount(),
                    isIncome = false,
                    id = RegularTransactionId("${user.id.value}-current-null-id-check"),
                    startDate = LocalDate.of(2026, 3, 1),
                    frequencyProperty = FrequencyProperty.Forever(),
                    recurrenceRule = RecurrenceRule.Monthly(1)
                )

                FakeFactory.regularTransactionState.init(
                    listOf(UserRegularTransaction(userId = user.id, transaction = regularTx, bookletIds = listOf(bookletId)))
                )

                val result = loadTransactionsForBookletForAMonthService.handle(LoadTransactionsForBookletForAMonthQuery(
                    user.id,
                    bookletId,
                    java.time.Month.MARCH,
                    2026,
                    startingMonth = java.time.Month.MARCH,
                    startingYear = 2026
                ))

                result.assertTrue { this.previsionalTransactions.all { tr -> tr.id != null } }
            }
        }

        @Test
        fun `Should expose virtual previsional transactions with null id for non current month`() {
            launchWithConnectedUserInstance {
                val bookletId = UUID.randomUUID()
                val booklet = Booklet(
                    amount = 1000.toAmount(),
                    label = "Future Month Virtual Id",
                    owner = user.toUser(),
                    id = bookletId
                )
                bookletState.init(listOf(BookletsByOwner(listOf(booklet), user.id)))

                val regularTx = RegularTransaction(
                    label = "Future month regular",
                    amount = 75.toAmount(),
                    isIncome = false,
                    id = RegularTransactionId("${user.id.value}-future-null-id-check"),
                    startDate = LocalDate.of(2026, 4, 1),
                    frequencyProperty = FrequencyProperty.Forever(),
                    recurrenceRule = RecurrenceRule.Monthly(1)
                )

                FakeFactory.regularTransactionState.init(
                    listOf(UserRegularTransaction(userId = user.id, transaction = regularTx, bookletIds = listOf(bookletId)))
                )

                val result = loadTransactionsForBookletForAMonthService.handle(LoadTransactionsForBookletForAMonthQuery(
                    user.id,
                    bookletId,
                    java.time.Month.APRIL,
                    2026,
                    startingMonth = java.time.Month.MARCH,
                    startingYear = 2026
                ))

                result.assertTrue { this.previsionalTransactions.any { tr -> tr.id == null } }
            }
        }

        @Test
        fun `Explicit date range should include start and exclude end plus one day for transactions`() {
            launchWithConnectedUserInstance {
                val bookletId = UUID.randomUUID()
                val booklet = Booklet(
                    amount = 1000.toAmount(),
                    label = "Explicit Range Tx",
                    owner = user.toUser(),
                    id = bookletId
                )
                bookletState.init(listOf(BookletsByOwner(listOf(booklet), user.id)))

                val txAtStart = Transaction(
                    id = UUID.randomUUID(),
                    label = "At start",
                    date = LocalDate.of(2026, 3, 28),
                    amount = 100.toAmount(),
                    isIncome = false,
                    isPreview = false
                )
                val txAtEnd = Transaction(
                    id = UUID.randomUUID(),
                    label = "At end",
                    date = LocalDate.of(2026, 4, 27),
                    amount = 50.toAmount(),
                    isIncome = false,
                    isPreview = false
                )
                val txAfterEnd = Transaction(
                    id = UUID.randomUUID(),
                    label = "After end",
                    date = LocalDate.of(2026, 4, 28),
                    amount = 75.toAmount(),
                    isIncome = false,
                    isPreview = false
                )

                FakeFactory.fakeTransactionRepository().init(
                    listOf(
                        IdBookletByTransaction(
                            IdUserBooklet(user.id, bookletId),
                            mutableListOf(txAtStart, txAtEnd, txAfterEnd)
                        )
                    )
                )
                FakeFactory.regularTransactionState.init(emptyList())

                val result = loadTransactionsForBookletForAMonthService.handle(LoadTransactionsForBookletForAMonthQuery(
                    userId = user.id,
                    bookletId = bookletId,
                    month = java.time.Month.APRIL,
                    year = 2026,
                    startDate = LocalDate.of(2026, 3, 28),
                    endDate = LocalDate.of(2026, 4, 27)
                ))

                result.assertTrue { this.currentTransactions.any { tr -> tr.label == "At start" } }
                result.assertTrue { this.currentTransactions.any { tr -> tr.label == "At end" } }
                result.assertTrue { this.currentTransactions.none { tr -> tr.label == "After end" } }
            }
        }

        @Test
        fun `Explicit date range should bound previsional sold to provided end date`() {
            launchWithConnectedUserInstance {
                val bookletId = UUID.randomUUID()
                val booklet = Booklet(
                    amount = 1000.toAmount(),
                    label = "Explicit Range Balance",
                    owner = user.toUser(),
                    id = bookletId
                )
                bookletState.init(listOf(BookletsByOwner(listOf(booklet), user.id)))

                val inRangePreview = Transaction(
                    id = UUID.randomUUID(),
                    label = "In range preview",
                    date = LocalDate.of(2026, 4, 27),
                    amount = 120.toAmount(),
                    isIncome = true,
                    isPreview = true
                )
                val outOfRangePreview = Transaction(
                    id = UUID.randomUUID(),
                    label = "Out of range preview",
                    date = LocalDate.of(2026, 4, 28),
                    amount = 80.toAmount(),
                    isIncome = true,
                    isPreview = true
                )

                FakeFactory.fakeTransactionRepository().init(
                    listOf(
                        IdBookletByTransaction(
                            IdUserBooklet(user.id, bookletId),
                            mutableListOf(inRangePreview, outOfRangePreview)
                        )
                    )
                )
                FakeFactory.regularTransactionState.init(emptyList())

                val result = loadBalancesForBookletForAMonthService.handle(LoadBalancesForBookletForAMonthQuery(
                    userId = user.id,
                    bookletId = bookletId,
                    month = java.time.Month.APRIL,
                    year = 2026,
                    startDate = LocalDate.of(2026, 3, 28),
                    endDate = LocalDate.of(2026, 4, 27)
                ))

                result.assertTrue { this.realSold == 1000.toAmount() }
                result.assertTrue { this.previewSold == 1120.toAmount() }
            }
        }

        @Test
        fun `Should not generate virtual transactions for a past explicit range with default settings`() {
            launchWithConnectedUserInstance {
                val bookletId = UUID.randomUUID()
                val booklet = Booklet(
                    amount = 1000.toAmount(),
                    label = "Past Default Range",
                    owner = user.toUser(),
                    id = bookletId
                )
                bookletState.init(listOf(BookletsByOwner(listOf(booklet), user.id)))

                val regularTx = RegularTransaction(
                    label = "Monthly salary",
                    amount = 3000.toAmount(),
                    isIncome = true,
                    id = RegularTransactionId("${user.id.value}-past-default-range"),
                    startDate = LocalDate.of(2026, 1, 1),
                    frequencyProperty = FrequencyProperty.Forever(),
                    recurrenceRule = RecurrenceRule.Monthly(15)
                )

                FakeFactory.regularTransactionState.init(
                    listOf(UserRegularTransaction(userId = user.id, transaction = regularTx, bookletIds = listOf(bookletId)))
                )
                FakeFactory.fakeTransactionRepository().init(emptyList())

                // Simulate: current date is 1st April 2026, user views March 2026 with default settings (1→31)
                val result = loadTransactionsForBookletForAMonthService.handle(LoadTransactionsForBookletForAMonthQuery(
                    userId = user.id,
                    bookletId = bookletId,
                    month = java.time.Month.MARCH,
                    year = 2026,
                    startingMonth = java.time.Month.APRIL,
                    startingYear = 2026,
                    startDate = LocalDate.of(2026, 3, 1),
                    endDate = LocalDate.of(2026, 3, 31)
                ))

                result.assertTrue { this.previsionalTransactions.isEmpty() }
            }
        }

        @Test
        fun `Should not generate virtual transactions for a past custom cycle range`() {
            launchWithConnectedUserInstance {
                val bookletId = UUID.randomUUID()
                val booklet = Booklet(
                    amount = 1000.toAmount(),
                    label = "Past Custom Cycle",
                    owner = user.toUser(),
                    id = bookletId
                )
                bookletState.init(listOf(BookletsByOwner(listOf(booklet), user.id)))

                val regularTx = RegularTransaction(
                    label = "Monthly salary",
                    amount = 3000.toAmount(),
                    isIncome = true,
                    id = RegularTransactionId("${user.id.value}-past-custom-cycle"),
                    startDate = LocalDate.of(2026, 1, 1),
                    frequencyProperty = FrequencyProperty.Forever(),
                    recurrenceRule = RecurrenceRule.Monthly(28)
                )

                FakeFactory.regularTransactionState.init(
                    listOf(UserRegularTransaction(userId = user.id, transaction = regularTx, bookletIds = listOf(bookletId)))
                )
                FakeFactory.fakeTransactionRepository().init(emptyList())

                // Simulate: current date is 1st April 2026, user views March cycle (28 Feb → 27 Mar)
                val result = loadTransactionsForBookletForAMonthService.handle(LoadTransactionsForBookletForAMonthQuery(
                    userId = user.id,
                    bookletId = bookletId,
                    month = java.time.Month.MARCH,
                    year = 2026,
                    startingMonth = java.time.Month.APRIL,
                    startingYear = 2026,
                    startDate = LocalDate.of(2026, 2, 28),
                    endDate = LocalDate.of(2026, 3, 27)
                ))

                result.assertTrue { this.previsionalTransactions.isEmpty() }
            }
        }

        @Test
        fun `Should generate virtual transaction for a future custom cycle range`() {
            launchWithConnectedUserInstance {
                val bookletId = UUID.randomUUID()
                val booklet = Booklet(
                    amount = 1000.toAmount(),
                    label = "Future Custom Cycle",
                    owner = user.toUser(),
                    id = bookletId
                )
                bookletState.init(listOf(BookletsByOwner(listOf(booklet), user.id)))

                val regularTx = RegularTransaction(
                    label = "Monthly salary",
                    amount = 3000.toAmount(),
                    isIncome = true,
                    id = RegularTransactionId("${user.id.value}-future-custom-cycle"),
                    startDate = LocalDate.of(2026, 1, 1),
                    frequencyProperty = FrequencyProperty.Forever(),
                    recurrenceRule = RecurrenceRule.Monthly(28)
                )

                FakeFactory.regularTransactionState.init(
                    listOf(UserRegularTransaction(userId = user.id, transaction = regularTx, bookletIds = listOf(bookletId)))
                )
                FakeFactory.fakeTransactionRepository().init(emptyList())

                // Simulate: current date is 1st April 2026, user views May cycle (28 Apr → 27 May)
                val result = loadTransactionsForBookletForAMonthService.handle(LoadTransactionsForBookletForAMonthQuery(
                    userId = user.id,
                    bookletId = bookletId,
                    month = java.time.Month.MAY,
                    year = 2026,
                    startingMonth = java.time.Month.APRIL,
                    startingYear = 2026,
                    startDate = LocalDate.of(2026, 4, 28),
                    endDate = LocalDate.of(2026, 5, 27)
                ))

                result.assertTrue { this.previsionalTransactions.isNotEmpty() }
                result.assertTrue {
                    this.previsionalTransactions.any { tx ->
                        tx.date == LocalDate.of(2026, 4, 28) && tx.isPreview
                    }
                }
            }
        }

        @Test
        fun `Should not generate previsional for day outside cycle start boundary with custom date range`() {
            launchWithConnectedUserInstance {
                val bookletId = UUID.randomUUID()
                val booklet = Booklet(
                    amount = 1000.toAmount(),
                    label = "Cycle Offset Start Boundary",
                    owner = user.toUser(),
                    id = bookletId
                )
                bookletState.init(listOf(BookletsByOwner(listOf(booklet), user.id)))

                // Monthly-on-5: without fix, March 5 would be generated by the full-month MARCH loop.
                // With fix, March 5 is before the cycle start (March 28) → must NOT be generated.
                // April 5 is within [March 28, April 27] → must be generated.
                val regularTx = RegularTransaction(
                    label = "Rent",
                    amount = 800.toAmount(),
                    isIncome = false,
                    id = RegularTransactionId("${user.id.value}-rent-cycle-offset-start"),
                    startDate = LocalDate.of(2026, 1, 5),
                    frequencyProperty = FrequencyProperty.Forever(),
                    recurrenceRule = RecurrenceRule.Monthly(5)
                )

                FakeFactory.regularTransactionState.init(
                    listOf(UserRegularTransaction(userId = user.id, transaction = regularTx, bookletIds = listOf(bookletId)))
                )
                FakeFactory.fakeTransactionRepository().init(emptyList())

                // Current cycle: March 28 → April 27. startingMonth = APRIL so today = April N.
                // Works correctly for days 1-27 of April (today within cycle range).
                val result = loadTransactionsForBookletForAMonthService.handle(LoadTransactionsForBookletForAMonthQuery(
                    userId = user.id,
                    bookletId = bookletId,
                    month = java.time.Month.APRIL,
                    year = 2026,
                    startingMonth = java.time.Month.APRIL,
                    startingYear = 2026,
                    startDate = LocalDate.of(2026, 3, 28),
                    endDate = LocalDate.of(2026, 4, 27)
                ))

                result.assertTrue { this.previsionalTransactions.none { tx -> tx.date == LocalDate.of(2026, 3, 5) } }
                result.assertTrue { this.previsionalTransactions.any { tx -> tx.date == LocalDate.of(2026, 4, 5) } }
            }
        }

        @Test
        fun `Should not generate previsional for day outside cycle end boundary with custom date range`() {
            launchWithConnectedUserInstance {
                val bookletId = UUID.randomUUID()
                val booklet = Booklet(
                    amount = 1000.toAmount(),
                    label = "Cycle Offset End Boundary",
                    owner = user.toUser(),
                    id = bookletId
                )
                bookletState.init(listOf(BookletsByOwner(listOf(booklet), user.id)))

                // Monthly-on-29: without fix, April 29 would be generated by the full-month APRIL loop.
                // With fix, April 29 is after the cycle end (April 27) → must NOT be generated.
                // March 29 is within [March 28, April 27] → must be generated.
                val regularTx = RegularTransaction(
                    label = "Salary",
                    amount = 3000.toAmount(),
                    isIncome = true,
                    id = RegularTransactionId("${user.id.value}-salary-cycle-offset-end"),
                    startDate = LocalDate.of(2026, 1, 29),
                    frequencyProperty = FrequencyProperty.Forever(),
                    recurrenceRule = RecurrenceRule.Monthly(29)
                )

                FakeFactory.regularTransactionState.init(
                    listOf(UserRegularTransaction(userId = user.id, transaction = regularTx, bookletIds = listOf(bookletId)))
                )
                FakeFactory.fakeTransactionRepository().init(emptyList())

                // Current cycle: March 28 → April 27. startingMonth = APRIL so today = April N.
                // Works correctly for days 1-27 of April (today within cycle range).
                val result = loadTransactionsForBookletForAMonthService.handle(LoadTransactionsForBookletForAMonthQuery(
                    userId = user.id,
                    bookletId = bookletId,
                    month = java.time.Month.APRIL,
                    year = 2026,
                    startingMonth = java.time.Month.APRIL,
                    startingYear = 2026,
                    startDate = LocalDate.of(2026, 3, 28),
                    endDate = LocalDate.of(2026, 4, 27)
                ))

                result.assertTrue { this.previsionalTransactions.none { tx -> tx.date == LocalDate.of(2026, 4, 29) } }
                result.assertTrue { this.previsionalTransactions.any { tx -> tx.date == LocalDate.of(2026, 3, 29) } }
            }
        }

        @Test
        fun `loadTransactions should signal hasRegenerableTransactions when a month is excluded for a regular transaction`() {
            launchWithConnectedUserInstance {
                val bookletId = booklet.id!!
                val bookletInstance = Booklet(
                    amount = 1000.toAmount(),
                    label = "Test Booklet",
                    owner = user.toUser(),
                    id = bookletId
                )
                bookletState.init(listOf(BookletsByOwner(listOf(bookletInstance), user.id)))

                val regularTx = RegularTransaction(
                    label = "Loyer",
                    amount = 900.toAmount(),
                    isIncome = false,
                    id = RegularTransactionId("${user.id.value}-has-regen-test"),
                    startDate = LocalDate.of(2024, 1, 1),
                    frequencyProperty = FrequencyProperty.Forever(),
                    recurrenceRule = RecurrenceRule.Monthly(5)
                )
                FakeFactory.regularTransactionState.init(
                    listOf(UserRegularTransaction(userId = user.id, transaction = regularTx, bookletIds = listOf(bookletId)))
                )
                FakeFactory.fakeTransactionRepository().init(emptyList())

                val currentDate = LocalDate.now()
                FakeFactory.trackerRepository().markMonthAsExcluded(
                    regularTx.id, bookletId, currentDate.year, currentDate.month
                )
                FakeFactory.fakeTransactionRepository().init(emptyList())

                val result = loadTransactionsForBookletForAMonthService.handle(LoadTransactionsForBookletForAMonthQuery(
                    userId = user.id,
                    bookletId = bookletId,
                    month = currentDate.month,
                    year = currentDate.year
                ))

                result.assertTrue { this.hasRegenerableTransactions }
            }
        }

        @Test
        fun `loadTransactions should not signal hasRegenerableTransactions when the excluded month is a past month`() {
            launchWithConnectedUserInstance {
                val bookletId = booklet.id!!
                val bookletInstance = Booklet(
                    amount = 1000.toAmount(),
                    label = "Test Booklet Past",
                    owner = user.toUser(),
                    id = bookletId
                )
                bookletState.init(listOf(BookletsByOwner(listOf(bookletInstance), user.id)))

                val regularTx = RegularTransaction(
                    label = "Loyer",
                    amount = 900.toAmount(),
                    isIncome = false,
                    id = RegularTransactionId("${user.id.value}-past-regen-test"),
                    startDate = LocalDate.of(2024, 1, 1),
                    frequencyProperty = FrequencyProperty.Forever(),
                    recurrenceRule = RecurrenceRule.Monthly(5)
                )
                FakeFactory.regularTransactionState.init(
                    listOf(UserRegularTransaction(userId = user.id, transaction = regularTx, bookletIds = listOf(bookletId)))
                )
                FakeFactory.fakeTransactionRepository().init(emptyList())

                // Mark a past month as excluded
                FakeFactory.trackerRepository().markMonthAsExcluded(
                    regularTx.id, bookletId, 2024, java.time.Month.FEBRUARY
                )

                // Load for that same past month
                val result = loadTransactionsForBookletForAMonthService.handle(LoadTransactionsForBookletForAMonthQuery(
                    userId = user.id,
                    bookletId = bookletId,
                    month = java.time.Month.FEBRUARY,
                    year = 2024,
                    startingMonth = java.time.Month.JANUARY,
                    startingYear = 2025
                ))

                result.assertTrue { !this.hasRegenerableTransactions }
            }
        }

        @Test
        fun `loadTransactions should not signal hasRegenerableTransactions when no month is excluded`() {
            launchWithConnectedUserInstance {
                val bookletId = booklet.id!!
                val bookletInstance = Booklet(
                    amount = 1000.toAmount(),
                    label = "Test Booklet",
                    owner = user.toUser(),
                    id = bookletId
                )
                bookletState.init(listOf(BookletsByOwner(listOf(bookletInstance), user.id)))

                val regularTx = RegularTransaction(
                    label = "Loyer",
                    amount = 900.toAmount(),
                    isIncome = false,
                    id = RegularTransactionId("${user.id.value}-no-regen-test"),
                    startDate = LocalDate.of(2024, 1, 1),
                    frequencyProperty = FrequencyProperty.Forever(),
                    recurrenceRule = RecurrenceRule.Monthly(5)
                )
                FakeFactory.regularTransactionState.init(
                    listOf(UserRegularTransaction(userId = user.id, transaction = regularTx, bookletIds = listOf(bookletId)))
                )
                FakeFactory.fakeTransactionRepository().init(emptyList())

                val result = loadTransactionsForBookletForAMonthService.handle(LoadTransactionsForBookletForAMonthQuery(
                    userId = user.id,
                    bookletId = bookletId,
                    month = java.time.Month.FEBRUARY,
                    year = 2024,
                    startingMonth = java.time.Month.JANUARY,
                    startingYear = 2025
                ))

                result.assertTrue { !this.hasRegenerableTransactions }
            }
        }

    }

    @Nested
    inner class LoadTransactionsWithPaginationTest {

        @Test
        fun `should return first page of transactions when 25 transactions exist`() {
            launchWithConnectedUserInstance {
                val bookletId = UUID.randomUUID()
                val booklet = Booklet(amount = 1000.toAmount(), label = "Paginated Booklet", owner = user.toUser(), id = bookletId)
                bookletState.init(listOf(BookletsByOwner(listOf(booklet), user.id)))

                val transactions = (1..25).map { i ->
                    Transaction(id = UUID.randomUUID(), label = "Transaction $i",
                        date = LocalDate.of(2025, 1, (i % 28) + 1), amount = 100.toAmount(),
                        isIncome = true, isPreview = false)
                }
                FakeFactory.fakeTransactionRepository().init(
                    listOf(IdBookletByTransaction(IdUserBooklet(user.id, bookletId), transactions.toMutableList()))
                )
                FakeFactory.regularTransactionState.init(emptyList())

                val result = loadTransactionsForBookletForAMonthService.handle(LoadTransactionsForBookletForAMonthQuery(
                    user.id, bookletId, java.time.Month.JANUARY, 2025,
                    startingMonth = java.time.Month.JANUARY, startingYear = 2025,
                    pageNumber = 0, pageSize = 10
                ))

                result.assertTrue { currentTransactions.size + previsionalTransactions.size == 10 }
                result.assertTrue { totalElements == 25L }
                result.assertTrue { totalPages == 3 }
                result.assertTrue { pageNumber == 0 }
                result.assertTrue { pageSize == 10 }
            }
        }

        @Test
        fun `should return last page with fewer items when total is not divisible by pageSize`() {
            launchWithConnectedUserInstance {
                val bookletId = UUID.randomUUID()
                val booklet = Booklet(amount = 1000.toAmount(), label = "Last Page Booklet", owner = user.toUser(), id = bookletId)
                bookletState.init(listOf(BookletsByOwner(listOf(booklet), user.id)))

                val transactions = (1..25).map { i ->
                    Transaction(id = UUID.randomUUID(), label = "Transaction $i",
                        date = LocalDate.of(2025, 1, (i % 28) + 1), amount = 100.toAmount(),
                        isIncome = true, isPreview = false)
                }
                FakeFactory.fakeTransactionRepository().init(
                    listOf(IdBookletByTransaction(IdUserBooklet(user.id, bookletId), transactions.toMutableList()))
                )
                FakeFactory.regularTransactionState.init(emptyList())

                val result = loadTransactionsForBookletForAMonthService.handle(LoadTransactionsForBookletForAMonthQuery(
                    user.id, bookletId, java.time.Month.JANUARY, 2025,
                    startingMonth = java.time.Month.JANUARY, startingYear = 2025,
                    pageNumber = 2, pageSize = 10
                ))

                result.assertTrue { currentTransactions.size + previsionalTransactions.size == 5 }
                result.assertTrue { totalElements == 25L }
                result.assertTrue { totalPages == 3 }
            }
        }

        @Test
        fun `should use default page=0 and size=10 when no pagination params are given`() {
            launchWithConnectedUserInstance {
                val bookletId = UUID.randomUUID()
                val booklet = Booklet(amount = 1000.toAmount(), label = "Default Pagination Booklet", owner = user.toUser(), id = bookletId)
                bookletState.init(listOf(BookletsByOwner(listOf(booklet), user.id)))

                val transactions = (1..15).map { i ->
                    Transaction(id = UUID.randomUUID(), label = "Transaction $i",
                        date = LocalDate.of(2025, 1, (i % 28) + 1), amount = 100.toAmount(),
                        isIncome = true, isPreview = false)
                }
                FakeFactory.fakeTransactionRepository().init(
                    listOf(IdBookletByTransaction(IdUserBooklet(user.id, bookletId), transactions.toMutableList()))
                )
                FakeFactory.regularTransactionState.init(emptyList())

                val result = loadTransactionsForBookletForAMonthService.handle(LoadTransactionsForBookletForAMonthQuery(
                    user.id, bookletId, java.time.Month.JANUARY, 2025,
                    startingMonth = java.time.Month.JANUARY, startingYear = 2025
                ))

                result.assertTrue { pageNumber == 0 }
                result.assertTrue { pageSize == 10 }
                result.assertTrue { currentTransactions.size + previsionalTransactions.size == 10 }
                result.assertTrue { totalElements == 15L }
            }
        }

        @Test
        fun `should return empty transaction list when requested page is out of range`() {
            launchWithConnectedUserInstance {
                val bookletId = UUID.randomUUID()
                val booklet = Booklet(amount = 1000.toAmount(), label = "Out of Range Booklet", owner = user.toUser(), id = bookletId)
                bookletState.init(listOf(BookletsByOwner(listOf(booklet), user.id)))

                val transactions = (1..5).map { i ->
                    Transaction(id = UUID.randomUUID(), label = "Transaction $i",
                        date = LocalDate.of(2025, 1, i), amount = 100.toAmount(),
                        isIncome = true, isPreview = false)
                }
                FakeFactory.fakeTransactionRepository().init(
                    listOf(IdBookletByTransaction(IdUserBooklet(user.id, bookletId), transactions.toMutableList()))
                )
                FakeFactory.regularTransactionState.init(emptyList())

                val result = loadTransactionsForBookletForAMonthService.handle(LoadTransactionsForBookletForAMonthQuery(
                    user.id, bookletId, java.time.Month.JANUARY, 2025,
                    startingMonth = java.time.Month.JANUARY, startingYear = 2025,
                    pageNumber = 5, pageSize = 10
                ))

                result.assertTrue { currentTransactions.isEmpty() && previsionalTransactions.isEmpty() }
                result.assertTrue { totalElements == 5L }
            }
        }

        @Test
        fun `should compute realSold on ALL transactions regardless of requested page`() {
            launchWithConnectedUserInstance {
                val bookletId = UUID.randomUUID()
                val booklet = Booklet(amount = 1000.toAmount(), label = "Balance Invariance Booklet", owner = user.toUser(), id = bookletId)
                bookletState.init(listOf(BookletsByOwner(listOf(booklet), user.id)))

                val transactions = (1..20).map { i ->
                    Transaction(id = UUID.randomUUID(), label = "Income $i",
                        date = LocalDate.of(2025, 1, (i % 28) + 1), amount = 100.toAmount(),
                        isIncome = true, isPreview = false)
                }
                FakeFactory.fakeTransactionRepository().init(
                    listOf(IdBookletByTransaction(IdUserBooklet(user.id, bookletId), transactions.toMutableList()))
                )
                FakeFactory.regularTransactionState.init(emptyList())

                val page0Result = loadTransactionsForBookletForAMonthService.handle(LoadTransactionsForBookletForAMonthQuery(
                    user.id, bookletId, java.time.Month.JANUARY, 2025,
                    startingMonth = java.time.Month.JANUARY, startingYear = 2025,
                    pageNumber = 0, pageSize = 10
                ))
                val page1Result = loadTransactionsForBookletForAMonthService.handle(LoadTransactionsForBookletForAMonthQuery(
                    user.id, bookletId, java.time.Month.JANUARY, 2025,
                    startingMonth = java.time.Month.JANUARY, startingYear = 2025,
                    pageNumber = 1, pageSize = 10
                ))

                // realSold = booklet.amount (stored balance) — unaffected by pagination
                page0Result.assertTrue { realSold == page1Result.mapNotNullOrFailure()!!.realSold }
            }
        }
    }

    @Nested
    inner class RegenerateDeletedPrevisionalTransactionsTest {

        private fun buildRegularTransaction(
            userId: UserId,
            bookletId: UUID,
            dayOfMonth: Int = 5,
            startDate: LocalDate = LocalDate.of(2024, 1, 1)
        ): RegularTransaction {
            val tx = RegularTransaction(
                label = "Loyer",
                amount = 900.toAmount(),
                isIncome = false,
                id = RegularTransactionId("${userId.value}-regen-test"),
                startDate = startDate,
                frequencyProperty = FrequencyProperty.Forever(),
                recurrenceRule = RecurrenceRule.Monthly(dayOfMonth)
            )
            FakeFactory.regularTransactionState.init(
                listOf(UserRegularTransaction(userId = userId, transaction = tx, bookletIds = listOf(bookletId)))
            )
            return tx
        }

        @Test
        fun `regenerate for a future month should return virtual transactions and unmark the tracker`() {
            launchWithConnectedUserInstance {
                val futureDate = LocalDate.now().plusMonths(2)
                val bookletId = booklet.id!!
                val bookletInstance = Booklet(
                    amount = 1000.toAmount(),
                    label = "Future Regen Booklet",
                    owner = user.toUser(),
                    id = bookletId
                )
                bookletState.init(listOf(BookletsByOwner(listOf(bookletInstance), user.id)))

                val regularTx = buildRegularTransaction(user.id, bookletId, dayOfMonth = 5)

                FakeFactory.trackerRepository().markMonthAsExcluded(
                    regularTx.id, bookletId, futureDate.year, futureDate.month
                )
                FakeFactory.fakeTransactionRepository().init(emptyList())

                val result = regenerateDeletedPrevisionalTransactionsService.handle(RegenerateDeletedPrevisionalTransactionsCommand(
                    userId = user.id,
                    bookletId = bookletId,
                    month = futureDate.month,
                    year = futureDate.year
                ))

                result.assertTrue { this.isNotEmpty() }
                result.assertTrue { this.any { it.label == "Loyer" } }

                val tracker = FakeFactory.trackerRepository().findTracker(regularTx.id, bookletId)
                assertFalse(tracker?.excludedMonths?.contains(YearMonth.from(futureDate)) == true,
                    "Future month should no longer be excluded after regeneration")
            }
        }

        @Test
        fun `regenerate for a past month should return empty list without modifying the tracker`() {
            launchWithConnectedUserInstance {
                val bookletId = booklet.id!!
                val bookletInstance = Booklet(
                    amount = 1000.toAmount(),
                    label = "Past Regen Booklet",
                    owner = user.toUser(),
                    id = bookletId
                )
                bookletState.init(listOf(BookletsByOwner(listOf(bookletInstance), user.id)))

                val regularTx = buildRegularTransaction(user.id, bookletId, dayOfMonth = 5)

                val pastYearMonth = YearMonth.of(2024, java.time.Month.MARCH)
                FakeFactory.trackerRepository().markMonthAsExcluded(
                    regularTx.id, bookletId, pastYearMonth.year, pastYearMonth.month
                )

                val result = regenerateDeletedPrevisionalTransactionsService.handle(RegenerateDeletedPrevisionalTransactionsCommand(
                    userId = user.id,
                    bookletId = bookletId,
                    month = pastYearMonth.month,
                    year = pastYearMonth.year
                ))

                result.assertTrue { this.isEmpty() }

                val tracker = FakeFactory.trackerRepository().findTracker(regularTx.id, bookletId)
                assertTrue(tracker?.excludedMonths?.contains(pastYearMonth) == true,
                    "Past month tracker exclusion must remain unchanged")
            }
        }

        @Test
        fun `regenerate for a non-existing booklet should return BOOKLET_NOT_FOUND`() {
            launchWithConnectedUserInstance {
                FakeFactory.regularTransactionState.init(emptyList())
                val result = regenerateDeletedPrevisionalTransactionsService.handle(RegenerateDeletedPrevisionalTransactionsCommand(
                    userId = user.id,
                    bookletId = UUID.randomUUID(),
                    month = java.time.Month.JANUARY,
                    year = 2024
                ))
                result.assertFailure(ResultState.BOOKLET_NOT_FOUND)
            }
        }

        @Test
        fun `regenerate for the current month should recreate previsional transactions`() {
            launchWithConnectedUserInstance {
                val currentDate = LocalDate.now()
                val bookletId = booklet.id!!
                val bookletInstance = Booklet(
                    amount = 1000.toAmount(),
                    label = "Regen Booklet",
                    owner = user.toUser(),
                    id = bookletId
                )
                bookletState.init(listOf(BookletsByOwner(listOf(bookletInstance), user.id)))

                val regularTx = buildRegularTransaction(user.id, bookletId, dayOfMonth = 1)
                FakeFactory.fakeTransactionRepository().init(emptyList())

                FakeFactory.trackerRepository().markMonthAsExcluded(
                    regularTx.id, bookletId, currentDate.year, currentDate.month
                )

                val result = regenerateDeletedPrevisionalTransactionsService.handle(RegenerateDeletedPrevisionalTransactionsCommand(
                    userId = user.id,
                    bookletId = bookletId,
                    month = currentDate.month,
                    year = currentDate.year
                ))

                result.assertTrue { this.isNotEmpty() }
                result.assertTrue { this.any { it.label == "Loyer" && it.isPreview } }

                val tracker = FakeFactory.trackerRepository().findTracker(regularTx.id, bookletId)
                assertFalse(tracker?.excludedMonths?.contains(YearMonth.from(currentDate)) == true,
                    "Current month should no longer be excluded after regeneration")
            }
        }

        @Test
        fun `regenerate should not create duplicate if confirmed transaction already exists for that month`() {
            launchWithConnectedUserInstance {
                val currentDate = LocalDate.now()
                val bookletId = booklet.id!!
                val bookletInstance = Booklet(
                    amount = 1000.toAmount(),
                    label = "No Dup Booklet",
                    owner = user.toUser(),
                    id = bookletId
                )
                bookletState.init(listOf(BookletsByOwner(listOf(bookletInstance), user.id)))

                val regularTx = buildRegularTransaction(user.id, bookletId, dayOfMonth = 1)

                // Existing confirmed transaction for same regular tx + current month
                val confirmed = Transaction(
                    id = UUID.randomUUID(),
                    label = "Loyer",
                    date = currentDate.withDayOfMonth(1),
                    amount = 900.toAmount(),
                    isIncome = false,
                    isPreview = false,
                    regularTransactionId = regularTx.id
                )
                FakeFactory.fakeTransactionRepository().init(
                    listOf(IdBookletByTransaction(IdUserBooklet(user.id, bookletId), mutableListOf(confirmed)))
                )

                // Month is excluded (user deleted the preview before confirming)
                FakeFactory.trackerRepository().markMonthAsExcluded(
                    regularTx.id, bookletId, currentDate.year, currentDate.month
                )

                val result = regenerateDeletedPrevisionalTransactionsService.handle(RegenerateDeletedPrevisionalTransactionsCommand(
                    userId = user.id,
                    bookletId = bookletId,
                    month = currentDate.month,
                    year = currentDate.year
                ))

                // No new transaction should be generated because a confirmed one already exists
                result.assertTrue { this.isEmpty() }
            }
        }

        @Test
        fun `regenerate should not create duplicate if preview transaction already exists for that month`() {
            launchWithConnectedUserInstance {
                val currentDate = LocalDate.now()
                val bookletId = booklet.id!!
                val bookletInstance = Booklet(
                    amount = 1000.toAmount(),
                    label = "No Dup Preview Booklet",
                    owner = user.toUser(),
                    id = bookletId
                )
                bookletState.init(listOf(BookletsByOwner(listOf(bookletInstance), user.id)))

                val regularTx = buildRegularTransaction(user.id, bookletId, dayOfMonth = 1)

                // Existing preview transaction for same regular tx + current month
                val existingPreview = Transaction(
                    id = UUID.randomUUID(),
                    label = "Loyer",
                    date = currentDate.withDayOfMonth(1),
                    amount = 900.toAmount(),
                    isIncome = false,
                    isPreview = true,
                    regularTransactionId = regularTx.id
                )
                FakeFactory.fakeTransactionRepository().init(
                    listOf(IdBookletByTransaction(IdUserBooklet(user.id, bookletId), mutableListOf(existingPreview)))
                )

                val result = regenerateDeletedPrevisionalTransactionsService.handle(RegenerateDeletedPrevisionalTransactionsCommand(
                    userId = user.id,
                    bookletId = bookletId,
                    month = currentDate.month,
                    year = currentDate.year
                ))

                result.assertTrue { this.isEmpty() }
            }
        }

        @Test
        fun `regenerate should only unmark the requested month — other excluded months remain excluded`() {
            launchWithConnectedUserInstance {
                val currentDate = LocalDate.now()
                val nextMonthDate = currentDate.plusMonths(1)
                val bookletId = booklet.id!!
                val bookletInstance = Booklet(
                    amount = 1000.toAmount(),
                    label = "Only One Month Booklet",
                    owner = user.toUser(),
                    id = bookletId
                )
                bookletState.init(listOf(BookletsByOwner(listOf(bookletInstance), user.id)))

                val regularTx = buildRegularTransaction(user.id, bookletId, dayOfMonth = 1)
                FakeFactory.fakeTransactionRepository().init(emptyList())

                // Exclude current month and next month
                FakeFactory.trackerRepository().markMonthAsExcluded(
                    regularTx.id, bookletId, currentDate.year, currentDate.month
                )
                FakeFactory.trackerRepository().markMonthAsExcluded(
                    regularTx.id, bookletId, nextMonthDate.year, nextMonthDate.month
                )

                // Only regenerate current month
                regenerateDeletedPrevisionalTransactionsService.handle(RegenerateDeletedPrevisionalTransactionsCommand(
                    userId = user.id,
                    bookletId = bookletId,
                    month = currentDate.month,
                    year = currentDate.year
                )).assertSuccess()

                val tracker = FakeFactory.trackerRepository().findTracker(regularTx.id, bookletId)
                assertFalse(
                    tracker?.excludedMonths?.contains(YearMonth.from(currentDate)) == true,
                    "Current month should no longer be excluded"
                )
                assertTrue(
                    tracker?.excludedMonths?.contains(YearMonth.from(nextMonthDate)) == true,
                    "Next month should still be excluded"
                )
            }
        }
    }
}

