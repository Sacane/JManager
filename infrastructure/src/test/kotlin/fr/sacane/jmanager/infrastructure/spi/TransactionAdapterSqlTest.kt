package fr.sacane.jmanager.infrastructure.spi

import fr.sacane.jmanager.domain.models.Booklet
import fr.sacane.jmanager.domain.models.Amount
import fr.sacane.jmanager.domain.models.Tag
import fr.sacane.jmanager.domain.models.UserId
import fr.sacane.jmanager.domain.models.transaction.Transaction
import fr.sacane.jmanager.infrastructure.api.AuthenticatedUserTest
import fr.sacane.jmanager.infrastructure.api.setup.BookletStateTestAdapter
import fr.sacane.jmanager.infrastructure.spi.adapters.transaction.TransactionRepositoryJpaAdapter
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.TestPropertySource
import java.time.LocalDate


@TestPropertySource(locations = ["classpath:application-test.properties"])
@SpringBootTest
class TransactionAdapterSqlTest(
    @Autowired private val transactionRepositoryJpaAdapter: TransactionRepositoryJpaAdapter,
    @Autowired private val bookletStateTestAdapter: BookletStateTestAdapter
): AuthenticatedUserTest() {

    @Nested
    inner class PersistTest {

        @Test
        fun `Persist a transaction must return success`() {
            val transaction = Transaction(
                amount = Amount(10.0.toLong()),
                label = "test",
                date = LocalDate.now(),
                isIncome = false,
                tag = Tag.Personal("test"),
                id = null
            )
            val booklet = Booklet(
                label = "test",
                amount = Amount(10.0.toLong()),
                owner = user,
            )
            bookletStateTestAdapter.init(listOf(booklet))
            val result = transactionRepositoryJpaAdapter.persist(userId = user!!.id, booklet.label, transaction)
            assertNotNull(result?.id)
        }

        @Test
        fun `Persist a transaction with a null User ID must return null`() {
            val transaction = Transaction(
                amount = Amount(10.0.toLong()),
                label = "test",
                date = LocalDate.now(),
                isIncome = false,
                tag = Tag.Personal("test"),
                id = null
            )
            val booklet = Booklet(
                label = "test",
                amount = Amount(10.0.toLong()),
                owner = user,
            )
            bookletStateTestAdapter.init(listOf(booklet))
            val result = transactionRepositoryJpaAdapter.persist(userId = UserId(null), booklet.label, transaction)
            assertNull(result)
        }

        @Test
        fun `Persist a transaction with an unknown booklet label must return null`() {
            val transaction = Transaction(
                amount = Amount(10.0.toLong()),
                label = "test",
                date = LocalDate.now(),
                isIncome = false,
                tag = Tag.Personal("test"),
                id = null
            )
            val booklet = Booklet(
                label = "test",
                amount = Amount(10.0.toLong()),
                owner = user,
            )
            bookletStateTestAdapter.init(listOf(booklet))
            val result = transactionRepositoryJpaAdapter.persist(userId = user!!.id, "unknown", transaction)
            assertNull(result)
        }
    }
}
