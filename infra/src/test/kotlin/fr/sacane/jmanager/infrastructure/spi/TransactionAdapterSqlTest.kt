package fr.sacane.jmanager.infrastructure.spi

import fr.sacane.jmanager.domain.models.*
import fr.sacane.jmanager.infrastructure.api.AuthenticatedUserTest
import fr.sacane.jmanager.infrastructure.api.setup.AccountStateTestAdapter
import fr.sacane.jmanager.infrastructure.spi.adapters.SqlTransactionAdapter
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
                _amount = Amount(10.0.toLong()),
                label = "test",
                date = LocalDate.now(),
                isIncome = false,
                tag = Tag("test"),
                id = null
            )
            val account = Account(
                labelAccount = "test",
                amount = Amount(10.0.toLong()),
                owner = user,
            )
            accountStateTestAdapter.init(listOf(account))
            val result = sqlTransactionAdapter.persist(userId = user!!.id, account.label, transaction)
            assertNotNull(result?.id)
        }

        @Test
        fun `Persist a transaction with a null User ID must return null`() {
            val transaction = Transaction(
                _amount = Amount(10.0.toLong()),
                label = "test",
                date = LocalDate.now(),
                isIncome = false,
                tag = Tag("test"),
                id = null
            )
            val account = Account(
                labelAccount = "test",
                amount = Amount(10.0.toLong()),
                owner = user,
            )
            accountStateTestAdapter.init(listOf(account))
            val result = sqlTransactionAdapter.persist(userId = UserId(null), account.label, transaction)
            assertNull(result)
        }

        @Test
        fun `Persist a transaction with an unknown account label must return null`() {
            val transaction = Transaction(
                _amount = Amount(10.0.toLong()),
                label = "test",
                date = LocalDate.now(),
                isIncome = false,
                tag = Tag("test"),
                id = null
            )
            val account = Account(
                labelAccount = "test",
                amount = Amount(10.0.toLong()),
                owner = user,
            )
            accountStateTestAdapter.init(listOf(account))
            val result = sqlTransactionAdapter.persist(userId = UserId(null), "unknown", transaction)
            assertNull(result)
        }
    }
}