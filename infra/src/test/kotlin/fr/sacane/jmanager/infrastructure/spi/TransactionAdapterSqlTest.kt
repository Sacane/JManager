package fr.sacane.jmanager.infrastructure.spi

import fr.sacane.jmanager.domain.models.Booklet
import fr.sacane.jmanager.domain.models.Amount
import fr.sacane.jmanager.domain.models.Tag
import fr.sacane.jmanager.domain.models.UserId
import fr.sacane.jmanager.domain.models.transaction.Transaction
import fr.sacane.jmanager.infrastructure.api.AuthenticatedUserTest
import fr.sacane.jmanager.infrastructure.api.setup.AccountStateTestAdapter
import fr.sacane.jmanager.infrastructure.spi.adapters.transaction.SqlTransactionAdapter
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
    @Autowired private val sqlTransactionAdapter: SqlTransactionAdapter,
    @Autowired private val accountStateTestAdapter: AccountStateTestAdapter
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
                tag = Tag("test"),
                id = null
            )
            val booklet = Booklet(
                labelAccount = "test",
                amount = Amount(10.0.toLong()),
                owner = user,
            )
            accountStateTestAdapter.init(listOf(booklet))
            val result = sqlTransactionAdapter.persist(userId = user!!.id, booklet.label, transaction)
            assertNotNull(result?.id)
        }

        @Test
        fun `Persist a transaction with a null User ID must return null`() {
            val transaction = Transaction(
                amount = Amount(10.0.toLong()),
                label = "test",
                date = LocalDate.now(),
                isIncome = false,
                tag = Tag("test"),
                id = null
            )
            val booklet = Booklet(
                labelAccount = "test",
                amount = Amount(10.0.toLong()),
                owner = user,
            )
            accountStateTestAdapter.init(listOf(booklet))
            val result = sqlTransactionAdapter.persist(userId = UserId(null), booklet.label, transaction)
            assertNull(result)
        }

        @Test
        fun `Persist a transaction with an unknown account label must return null`() {
            val transaction = Transaction(
                amount = Amount(10.0.toLong()),
                label = "test",
                date = LocalDate.now(),
                isIncome = false,
                tag = Tag("test"),
                id = null
            )
            val booklet = Booklet(
                labelAccount = "test",
                amount = Amount(10.0.toLong()),
                owner = user,
            )
            accountStateTestAdapter.init(listOf(booklet))
            val result = sqlTransactionAdapter.persist(userId = UserId(null), "unknown", transaction)
            assertNull(result)
        }
    }
}