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
import java.util.UUID

/** Where the user placed each of their booklets, stored on the booklet row. */
@TestPropertySource(locations = ["classpath:application-test.properties"])
@SpringBootTest
class BookletOrderPersistenceIT(
    @Autowired private val bookletAdapter: BookletJpaRepositoryAdapter,
    @Autowired private val userAdapter: UserRepositoryJpaAdapter,
    @Autowired private val transactionAdapter: TransactionRepositoryJpaAdapter,
    @Autowired private val bookletStateTestAdapter: BookletStateTestAdapter,
) : AuthenticatedUserTest() {

    @AfterEach
    fun clear() = bookletStateTestAdapter.clear()

    @Test
    fun `shouldHaveNoPosition_untilTheBookletsAreOrdered`() {
        val (current, savings) = givenBooklets()

        assertThat(storedPositions()).containsEntry(current, null).containsEntry(savings, null)
    }

    // The booklets are read once before, as the list page does: the cached read must not survive the change.
    @Test
    fun `shouldStoreEachBookletAtItsIndex`() {
        val (current, savings) = givenBooklets()
        storedPositions()

        assertThat(bookletAdapter.updateDisplayPositions(listOf(savings, current))).isTrue()

        assertThat(storedPositions()).containsEntry(savings, 0).containsEntry(current, 1)
    }

    @Test
    fun `shouldReportFailure_forAnUnknownBooklet`() {
        val (current, _) = givenBooklets()

        assertThat(bookletAdapter.updateDisplayPositions(listOf(current, UUID.randomUUID()))).isFalse()
    }

    // Booking a transaction saves the whole booklet again: that must not lose its place.
    @Test
    fun `shouldKeepThePosition_whenTheBookletIsSavedAgain`() {
        val (current, savings) = givenBooklets()
        bookletAdapter.updateDisplayPositions(listOf(savings, current))

        bookletAdapter.upsert(bookletAdapter.findBookletByIdWithTransactions(current)!!)

        assertThat(storedPositions()).containsEntry(savings, 0).containsEntry(current, 1)
    }

    private fun givenBooklets(): Pair<UUID, UUID> {
        bookletStateTestAdapter.init(
            listOf(
                Booklet(label = "Compte courant", amount = Amount(100L), owner = user),
                Booklet(label = "Livret A", amount = Amount(100L), owner = user),
            )
        )
        return idOf("Compte courant") to idOf("Livret A")
    }

    private fun idOf(label: String): UUID = transactionAdapter.findBookletByLabelWithTransactions(label, user!!.id)!!.id!!

    private fun storedPositions(): Map<UUID, Int?> =
        userAdapter.findUserByIdWithBooklets(user!!.id)!!.booklets.associate { it.id!! to it.displayPosition }
}
