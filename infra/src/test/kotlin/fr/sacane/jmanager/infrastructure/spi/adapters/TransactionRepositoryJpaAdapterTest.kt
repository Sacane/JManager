package fr.sacane.jmanager.infrastructure.spi.adapters

import fr.sacane.jmanager.domain.models.Amount
import fr.sacane.jmanager.domain.models.Booklet
import fr.sacane.jmanager.domain.models.Tag
import fr.sacane.jmanager.domain.models.transaction.Transaction
import fr.sacane.jmanager.infrastructure.api.AuthenticatedUserTest
import fr.sacane.jmanager.infrastructure.api.setup.AccountStateTestAdapter
import fr.sacane.jmanager.infrastructure.spi.adapters.transaction.TransactionRepositoryJpaAdapter
import fr.sacane.jmanager.infrastructure.spi.repositories.TransactionJpaRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.TestPropertySource
import java.time.LocalDate
import java.time.Month

@TestPropertySource(locations = ["classpath:application-test.properties"])
@SpringBootTest
class TransactionRepositoryJpaAdapterTest(
    @Autowired private val transactionRepositoryJpaAdapter: TransactionRepositoryJpaAdapter,
    @Autowired private val accountStateTestAdapter: AccountStateTestAdapter,
    @Autowired private val transactionJpaRepository: TransactionJpaRepository
) : AuthenticatedUserTest() {

    @AfterEach
    fun clear() {
        // cleanup persisted data between tests
        transactionJpaRepository.deleteAll()
        accountStateTestAdapter.clear()
    }

    @Nested
    inner class SaveAndFindTest {

        @Test
        fun `save must persist transaction and be retrievable by id`() {
            val booklet = Booklet(labelAccount = "acct-save", amount = Amount(100L), owner = user)
            accountStateTestAdapter.init(listOf(booklet))

            val tx = Transaction(
                id = null,
                label = "tx-save",
                date = LocalDate.now(),
                amount = Amount(500L),
                isIncome = false,
                tag = null
            )

            val persistedAccount = transactionRepositoryJpaAdapter.findAccountWithSheetByLabelAndUser(booklet.label, user!!.id)
            val accountId = persistedAccount!!.id!!
            val saved = transactionRepositoryJpaAdapter.save(accountId, tx)
            assertThat(saved).isNotNull
            val id = saved!!.id
            assertThat(id).isNotNull

            val found = transactionRepositoryJpaAdapter.findTransactionById(id!!)
            assertThat(found).isNotNull
            assertThat(found!!.label).isEqualTo("tx-save")
        }

        @Test
        fun `deleteAllSheetsById must remove sheets from repository`() {
            val booklet = Booklet(labelAccount = "acct-del", amount = Amount(200L), owner = user)
            accountStateTestAdapter.init(listOf(booklet))

            val tx1 = Transaction(id = null, label = "t1", date = LocalDate.now(), amount = Amount(10L), isIncome = false)
            val tx2 = Transaction(id = null, label = "t2", date = LocalDate.now(), amount = Amount(20L), isIncome = true)

            val persistedAccount = transactionRepositoryJpaAdapter.findAccountWithSheetByLabelAndUser(booklet.label, user!!.id)

            val accountId = persistedAccount!!.id!!
            val s1 = transactionRepositoryJpaAdapter.save(accountId, tx1)
            val s2 = transactionRepositoryJpaAdapter.save(accountId, tx2)

            assertThat(s1).isNotNull
            assertThat(s2).isNotNull

            transactionRepositoryJpaAdapter.deleteAllSheetsById(listOf(s1!!.id!!, s2!!.id!!))

            val f1 = transactionRepositoryJpaAdapter.findTransactionById(s1.id!!)
            val f2 = transactionRepositoryJpaAdapter.findTransactionById(s2.id!!)

            assertThat(f1).isNull()
            assertThat(f2).isNull()
        }
    }

    @Nested
    inner class AccountAndListingTest {

        @Test
        fun `findAccountWithSheetByLabelAndUser must return account with sheets`() {
            val booklet = Booklet(labelAccount = "acct-list", amount = Amount(300L), owner = user)
            accountStateTestAdapter.init(listOf(booklet))

            val tx = Transaction(id = null, label = "list-tx", date = LocalDate.now(), amount = Amount(30L), isIncome = false, tag = Tag("t"))
            val persisted = transactionRepositoryJpaAdapter.persist(userId = user!!.id, accountLabel = booklet.label, transaction = tx)

            assertThat(persisted).isNotNull

            val account = transactionRepositoryJpaAdapter.findAccountWithSheetByLabelAndUser(booklet.label, user!!.id)
            assertThat(account).isNotNull
            assertThat(account!!.transactions).isNotEmpty
        }

        @Test
        fun `findTransactionsByBookletYearAndMonth must filter by month and year`() {
            val year = 2025
            val booklet = Booklet(labelAccount = "acct-month", amount = Amount(400L), owner = user)
            accountStateTestAdapter.init(listOf(booklet))

            val txJan1 = Transaction(id = null, label = "jan1", date = LocalDate.of(year, Month.JANUARY, 5), amount = Amount(10L), isIncome = false)
            val txJan2 = Transaction(id = null, label = "jan2", date = LocalDate.of(year, Month.JANUARY, 15), amount = Amount(20L), isIncome = true)
            val txDec = Transaction(id = null, label = "dec", date = LocalDate.of(year - 1, Month.DECEMBER, 25), amount = Amount(30L), isIncome = false)

            val persistedAccount = transactionRepositoryJpaAdapter.findAccountWithSheetByLabelAndUser(booklet.label, user!!.id)

            val accountId = persistedAccount!!.id!!

            transactionRepositoryJpaAdapter.save(accountId, txJan1)
            transactionRepositoryJpaAdapter.save(accountId, txJan2)
            transactionRepositoryJpaAdapter.save(accountId, txDec)

            val result = transactionRepositoryJpaAdapter.findTransactionsByBookletYearAndMonth(accountId, year, Month.JANUARY)
            assertThat(result).isNotNull
            assertThat(result!!.map { it.label }).containsExactlyInAnyOrder("jan1", "jan2")
        }
    }
}
