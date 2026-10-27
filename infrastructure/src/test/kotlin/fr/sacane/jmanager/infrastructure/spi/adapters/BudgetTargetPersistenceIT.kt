package fr.sacane.jmanager.infrastructure.spi.adapters

import fr.sacane.jmanager.domain.models.Amount
import fr.sacane.jmanager.domain.models.Booklet
import fr.sacane.jmanager.infrastructure.api.AuthenticatedUserTest
import fr.sacane.jmanager.infrastructure.api.setup.BookletStateTestAdapter
import fr.sacane.jmanager.infrastructure.spi.adapters.transaction.TransactionRepositoryJpaAdapter
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.TestPropertySource
import java.math.BigDecimal
import java.util.UUID

/** The budget target of a booklet (UX-23), stored on the booklet row. */
@TestPropertySource(locations = ["classpath:application-test.properties"])
@SpringBootTest
class BudgetTargetPersistenceIT(
    @Autowired private val bookletAdapter: BookletJpaRepositoryAdapter,
    @Autowired private val userAdapter: UserRepositoryJpaAdapter,
    @Autowired private val transactionAdapter: TransactionRepositoryJpaAdapter,
    @Autowired private val bookletStateTestAdapter: BookletStateTestAdapter,
) : AuthenticatedUserTest() {

    @AfterEach
    fun clear() = bookletStateTestAdapter.clear()

    @Test
    fun `shouldReturnTheTargetWithTheBooklet_onceRecorded`() {
        val bookletId = givenBooklet()

        assertThat(bookletAdapter.updateBudgetTarget(bookletId, BigDecimal("1500.50"))).isTrue()

        assertThat(storedTarget(bookletId)).isEqualByComparingTo("1500.50")
    }

    @Test
    fun `shouldForgetTheTarget_whenCleared`() {
        val bookletId = givenBooklet()
        bookletAdapter.updateBudgetTarget(bookletId, BigDecimal("1500"))

        bookletAdapter.updateBudgetTarget(bookletId, null)

        assertThat(storedTarget(bookletId)).isNull()
    }

    @Test
    fun `shouldReportNothingUpdated_forAnUnknownBooklet`() {
        assertThat(bookletAdapter.updateBudgetTarget(UUID.randomUUID(), BigDecimal("10"))).isFalse()
    }

    // Booking a transaction saves the whole booklet again: that must not erase its target.
    @Test
    fun `shouldKeepTheTarget_whenTheBookletIsSavedAgain`() {
        val bookletId = givenBooklet()
        bookletAdapter.updateBudgetTarget(bookletId, BigDecimal("1500"))

        val reloaded = bookletAdapter.findBookletByIdWithTransactions(bookletId)!!
        bookletAdapter.upsert(reloaded)

        assertThat(storedTarget(bookletId)).isEqualByComparingTo("1500")
    }

    private fun givenBooklet(): UUID {
        bookletStateTestAdapter.init(listOf(Booklet(label = "Compte courant", amount = Amount(100L), owner = user)))
        return transactionAdapter.findBookletByLabelWithTransactions("Compte courant", user!!.id)!!.id!!
    }

    private fun storedTarget(bookletId: UUID): BigDecimal? =
        userAdapter.findUserByIdWithBooklets(user!!.id)!!.booklets.single { it.id == bookletId }.budgetTarget
}
