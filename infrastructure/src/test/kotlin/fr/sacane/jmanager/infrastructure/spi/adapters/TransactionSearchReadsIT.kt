package fr.sacane.jmanager.infrastructure.spi.adapters

import fr.sacane.jmanager.domain.models.Amount
import fr.sacane.jmanager.domain.models.Booklet
import fr.sacane.jmanager.domain.models.BookletSummary
import fr.sacane.jmanager.domain.models.transaction.Transaction
import fr.sacane.jmanager.infrastructure.api.AuthenticatedUserTest
import fr.sacane.jmanager.infrastructure.api.setup.BookletStateTestAdapter
import fr.sacane.jmanager.infrastructure.spi.adapters.transaction.TransactionQueryRepositoryJpaAdapter
import fr.sacane.jmanager.infrastructure.spi.adapters.transaction.TransactionRepositoryJpaAdapter
import fr.sacane.jmanager.infrastructure.spi.repositories.TransactionJpaRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.TestPropertySource
import java.time.LocalDate
import java.util.UUID

/** The two light reads behind the global transaction search. */
@TestPropertySource(locations = ["classpath:application-test.properties"])
@SpringBootTest
class TransactionSearchReadsIT(
    @Autowired private val bookletAdapter: BookletJpaRepositoryAdapter,
    @Autowired private val transactionQueryAdapter: TransactionQueryRepositoryJpaAdapter,
    @Autowired private val transactionAdapter: TransactionRepositoryJpaAdapter,
    @Autowired private val bookletStateTestAdapter: BookletStateTestAdapter,
    @Autowired private val transactionJpaRepository: TransactionJpaRepository,
) : AuthenticatedUserTest() {

    @AfterEach
    fun clear() {
        transactionJpaRepository.deleteAll()
        bookletStateTestAdapter.clear()
    }

    @Test
    fun `shouldNameEveryBookletOfTheUser`() {
        bookletStateTestAdapter.init(listOf(booklet("Compte courant"), booklet("Livret A")))

        val summaries = bookletAdapter.findBookletSummariesForUser(user!!.id)

        assertThat(summaries.map { it.label }).containsExactlyInAnyOrder("Compte courant", "Livret A")
        assertThat(summaries).allSatisfy { assertThat(it).isInstanceOf(BookletSummary::class.java) }
        assertThat(summaries.map { it.id }).doesNotContainNull()
    }

    @Test
    fun `shouldNameNoBooklet_forAUserWithout`() {
        assertThat(bookletAdapter.findBookletSummariesForUser(fr.sacane.jmanager.domain.models.UserId(UUID.randomUUID())))
            .isEmpty()
    }

    @Test
    fun `shouldGiveTheOldestTransactionDateAcrossTheBooklets`() {
        bookletStateTestAdapter.init(listOf(booklet("Compte courant"), booklet("Livret A")))
        val current = idOf("Compte courant")
        val savings = idOf("Livret A")
        transactionAdapter.save(current, spent(LocalDate.of(2025, 3, 10)))
        transactionAdapter.save(savings, spent(LocalDate.of(2024, 11, 2)))
        transactionAdapter.save(savings, spent(LocalDate.of(2026, 1, 5)))

        assertThat(transactionQueryAdapter.findEarliestTransactionDate(listOf(current, savings))).isEqualTo(LocalDate.of(2024, 11, 2))
        assertThat(transactionQueryAdapter.findEarliestTransactionDate(listOf(current))).isEqualTo(LocalDate.of(2025, 3, 10))
    }

    @Test
    fun `shouldGiveNoDate_whenTheBookletsHoldNoTransaction`() {
        bookletStateTestAdapter.init(listOf(booklet("Compte courant")))

        assertThat(transactionQueryAdapter.findEarliestTransactionDate(listOf(idOf("Compte courant")))).isNull()
        assertThat(transactionQueryAdapter.findEarliestTransactionDate(emptyList())).isNull()
    }

    private fun booklet(label: String) = Booklet(label = label, amount = Amount(100L), owner = user)

    private fun idOf(label: String): UUID = transactionAdapter.findBookletByLabelWithTransactions(label, user!!.id)!!.id!!

    private fun spent(date: LocalDate) = Transaction(
        id = null,
        label = "Courses",
        date = date,
        amount = Amount(10L),
        isIncome = false,
    )
}
