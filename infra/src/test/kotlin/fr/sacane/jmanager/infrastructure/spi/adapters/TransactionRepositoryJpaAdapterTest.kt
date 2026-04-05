package fr.sacane.jmanager.infrastructure.spi.adapters

import fr.sacane.jmanager.domain.models.Amount
import fr.sacane.jmanager.domain.models.Booklet
import fr.sacane.jmanager.domain.models.Tag
import fr.sacane.jmanager.domain.models.transaction.Transaction
import fr.sacane.jmanager.infrastructure.api.AuthenticatedUserTest
import fr.sacane.jmanager.infrastructure.api.setup.BookletStateTestAdapter
import fr.sacane.jmanager.infrastructure.spi.adapters.transaction.TransactionRepositoryJpaAdapter
import fr.sacane.jmanager.infrastructure.spi.repositories.TransactionJpaRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.TestPropertySource
import java.time.LocalDate
import java.time.Month
import fr.sacane.jmanager.domain.models.defaultTags
import fr.sacane.jmanager.infrastructure.spi.repositories.DefaultTagPostgresRepository
import fr.sacane.jmanager.infrastructure.spi.entity.DefaultTagResource
import fr.sacane.jmanager.infrastructure.spi.adapters.utils.asResource

@TestPropertySource(locations = ["classpath:application-test.properties"])
@SpringBootTest
class TransactionRepositoryJpaAdapterTest : AuthenticatedUserTest() {

    @Autowired
    private lateinit var transactionRepositoryJpaAdapter: TransactionRepositoryJpaAdapter

    @Autowired
    private lateinit var bookletStateTestAdapter: BookletStateTestAdapter

    @Autowired
    private lateinit var transactionJpaRepository: TransactionJpaRepository

    @Autowired
    private lateinit var defaultTagPostgresRepository: DefaultTagPostgresRepository

    @BeforeEach
    fun ensureDefaultTagsClean() {
        // Remove duplicate default tags (same name) keeping the first
        val existingDefaults = defaultTagPostgresRepository.findAll().toList()
        existingDefaults.groupBy { it.name }.forEach { (_, group) ->
            if (group.size > 1) {
                group.drop(1).forEach { item ->
                    item.idTag?.let { defaultTagPostgresRepository.deleteById(it) }
                }
            }
        }
        // Ensure default tags exist (insert missing ones)
        val refreshed = defaultTagPostgresRepository.findAll().toList()
        for (t in defaultTags) {
            if (refreshed.firstOrNull { it.name == t.label } == null) {
                defaultTagPostgresRepository.save(t.asResource() as DefaultTagResource)
            }
        }
    }

    @AfterEach
    fun clear() {
        transactionJpaRepository.deleteAll()
        bookletStateTestAdapter.clear()
    }

    @Nested
    inner class SaveAndFindTest {

        @Test
        fun `save must persist transaction and be retrievable by id`() {
            val booklet = Booklet(label = "acct-save", amount = Amount(100L), owner = user)
            bookletStateTestAdapter.init(listOf(booklet))

            val tx = Transaction(
                id = null,
                label = "tx-save",
                date = LocalDate.now(),
                amount = Amount(500L),
                isIncome = false,
                tag = null
            )

            val persistedBooklet = transactionRepositoryJpaAdapter.findBookletByLabelWithSheets(booklet.label, user!!.id)
            val bookletId = persistedBooklet!!.id!!
            val saved = transactionRepositoryJpaAdapter.save(bookletId, tx)
            assertThat(saved).isNotNull
            val id = saved!!.id
            assertThat(id).isNotNull

            val found = transactionRepositoryJpaAdapter.findTransactionById(id!!)
            assertThat(found).isNotNull
            assertThat(found!!.label).isEqualTo("tx-save")
        }

        @Test
        fun `deleteAllSheetsById must remove sheets from repository`() {
            val booklet = Booklet(label = "acct-del", amount = Amount(200L), owner = user)
            bookletStateTestAdapter.init(listOf(booklet))

            val tx1 = Transaction(id = null, label = "t1", date = LocalDate.now(), amount = Amount(10L), isIncome = false)
            val tx2 = Transaction(id = null, label = "t2", date = LocalDate.now(), amount = Amount(20L), isIncome = true)

            val persistedBooklet = transactionRepositoryJpaAdapter.findBookletByLabelWithSheets(booklet.label, user!!.id)

            val bookletId = persistedBooklet!!.id!!
            val s1 = transactionRepositoryJpaAdapter.save(bookletId, tx1)
            val s2 = transactionRepositoryJpaAdapter.save(bookletId, tx2)

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
    inner class BookletAndListingTest {

        @Test
        fun `findBookletByLabelWithSheets must return booklet with sheets`() {
            val booklet = Booklet(label = "acct-list", amount = Amount(300L), owner = user)
            bookletStateTestAdapter.init(listOf(booklet))

            val tx = Transaction(id = null, label = "list-tx", date = LocalDate.now(), amount = Amount(30L), isIncome = false, tag = Tag("t"))
            val persisted = transactionRepositoryJpaAdapter.persist(userId = user!!.id, bookletLabel = booklet.label, transaction = tx)

            assertThat(persisted).isNotNull

            val retrieved = transactionRepositoryJpaAdapter.findBookletByLabelWithSheets(booklet.label, user!!.id)
            assertThat(retrieved).isNotNull
            assertThat(retrieved!!.transactions).isNotEmpty
        }

        @Test
        fun `findTransactionsByBookletYearAndMonth must filter by month and year`() {
            val year = 2025
            val booklet = Booklet(label = "acct-month", amount = Amount(400L), owner = user)
            bookletStateTestAdapter.init(listOf(booklet))

            val txJan1 = Transaction(id = null, label = "jan1", date = LocalDate.of(year, Month.JANUARY, 5), amount = Amount(10L), isIncome = false)
            val txJan2 = Transaction(id = null, label = "jan2", date = LocalDate.of(year, Month.JANUARY, 15), amount = Amount(20L), isIncome = true)
            val txDec = Transaction(id = null, label = "dec", date = LocalDate.of(year - 1, Month.DECEMBER, 25), amount = Amount(30L), isIncome = false)

            val persistedBooklet = transactionRepositoryJpaAdapter.findBookletByLabelWithSheets(booklet.label, user!!.id)

            val bookletId = persistedBooklet!!.id!!

            transactionRepositoryJpaAdapter.save(bookletId, txJan1)
            transactionRepositoryJpaAdapter.save(bookletId, txJan2)
            transactionRepositoryJpaAdapter.save(bookletId, txDec)

            val result = transactionRepositoryJpaAdapter.findTransactionsByBookletYearAndMonth(bookletId, year, Month.JANUARY)
            assertThat(result).isNotNull
            assertThat(result!!.map { it.label }).containsExactlyInAnyOrder("jan1", "jan2")
        }
    }

    @Nested
    inner class AdditionalMethodsTest {

        @Test
        fun `mapToRightTag should resolve default, personal and unknown tags correctly`() {
            val booklet = Booklet(label = "acct-maptag", amount = Amount(500L), owner = user)
            bookletStateTestAdapter.init(listOf(booklet))

            val uid = user!!.id

            val txUnknown = Transaction(id = null, label = "tx-unknown", date = LocalDate.now(), amount = Amount(5L), isIncome = false, tag = Tag.noneTag())
            val persisted = transactionRepositoryJpaAdapter.persist(userId = uid, bookletLabel = booklet.label, transaction = txUnknown)
            assertThat(persisted).isNotNull
            assertThat(persisted!!.tag).isNotNull
            assertThat(persisted.tag!!.label).isEqualTo(Tag.noneTag().label)

            val existingDefaults = defaultTagPostgresRepository.findAll().toList()
            existingDefaults.groupBy { it.name }.forEach { (_, group) ->
                if (group.size > 1) {
                    group.drop(1).forEach { item ->
                        item.idTag?.let { defaultTagPostgresRepository.deleteById(it) }
                    }
                }
            }
            val refreshed = defaultTagPostgresRepository.findAll().toList()
            for (t in defaultTags) {
                if (refreshed.firstOrNull { it.name == t.label } == null) {
                    defaultTagPostgresRepository.save(t.asResource() as DefaultTagResource)
                }
            }
            // sanity check: ensure the default tag was saved in DB
            val tagInDb = defaultTagPostgresRepository.findAll().firstOrNull { it.name == "Achat & Shopping" }
            assertThat(tagInDb).isNotNull

             // build transaction tag with the id from DB to force resolution by id
            val txDefault = Transaction(id = null, label = "tx-default", date = LocalDate.now(), amount = Amount(10L), isIncome = false, tag = Tag("Achat & Shopping",
                tagInDb?.idTag, isDefault = true))
            val bookletRef = transactionRepositoryJpaAdapter.findBookletByLabelWithSheets(booklet.label, uid)
            val bookletId = bookletRef!!.id!!
            val persistedDefault = transactionRepositoryJpaAdapter.save(bookletId, txDefault)
            assertThat(persistedDefault).isNotNull
            assertThat(persistedDefault!!.tag).isNotNull
            assertThat(persistedDefault.tag!!.label).isEqualTo("Achat & Shopping")

            val personalTag = Tag("my-personal", null, isDefault = false)
            val txPersonal = Transaction(id = null, label = "tx-personal", date = LocalDate.now(), amount = Amount(15L), isIncome = false, tag = personalTag)
            val persistedPersonal = transactionRepositoryJpaAdapter.persist(userId = uid, bookletLabel = booklet.label, transaction = txPersonal)
            assertThat(persistedPersonal).isNotNull
            assertThat(persistedPersonal!!.tag).isNotNull
        }

        @Test
        fun `findBookletByIdWithTransactions must return booklet populated with transactions`() {
            val booklet = Booklet(label = "acct-findById", amount = Amount(600L), owner = user)
            bookletStateTestAdapter.init(listOf(booklet))

            val uid = user!!.id

            val tx1 = Transaction(id = null, label = "fb1", date = LocalDate.now(), amount = Amount(1L), isIncome = false)
            val tx2 = Transaction(id = null, label = "fb2", date = LocalDate.now(), amount = Amount(2L), isIncome = false)

            val persisted = transactionRepositoryJpaAdapter.persist(userId = uid, bookletLabel = booklet.label, transaction = tx1)
            val persisted2 = transactionRepositoryJpaAdapter.persist(userId = uid, bookletLabel = booklet.label, transaction = tx2)

            // ensure persisted transactions were created
            assertThat(persisted).isNotNull
            assertThat(persisted2).isNotNull

            val retrievedBooklet = transactionRepositoryJpaAdapter.findBookletByLabelWithSheets(booklet.label, uid)
            assertThat(retrievedBooklet).isNotNull
            val bookletId = retrievedBooklet!!.id!!

            val loaded = transactionRepositoryJpaAdapter.findBookletByIdWithTransactions(bookletId)
            assertThat(loaded).isNotNull
            assertThat(loaded!!.transactions).isNotEmpty
            assertThat(loaded.transactions.map { it.label }).containsExactlyInAnyOrder("fb1", "fb2")
        }

        @Test
        fun `findTransactionsByBookletId should return all persisted transactions for booklet`() {
            val booklet = Booklet(label = "acct-listAll", amount = Amount(700L), owner = user)
            bookletStateTestAdapter.init(listOf(booklet))

            val txA = Transaction(id = null, label = "A", date = LocalDate.now(), amount = Amount(7L), isIncome = false)
            val txB = Transaction(id = null, label = "B", date = LocalDate.now(), amount = Amount(8L), isIncome = false)

            val uid = user!!.id
            val retrievedBooklet = transactionRepositoryJpaAdapter.findBookletByLabelWithSheets(booklet.label, uid)
            val bookletId = retrievedBooklet!!.id!!

            val savedA = transactionRepositoryJpaAdapter.save(bookletId, txA)
            val savedB = transactionRepositoryJpaAdapter.save(bookletId, txB)
            assertThat(savedA).isNotNull
            assertThat(savedB).isNotNull

            val list = transactionRepositoryJpaAdapter.findTransactionsByBookletId(bookletId)
            assertThat(list).isNotNull
            assertThat(list!!.map { it.label }).containsExactlyInAnyOrder("A", "B")
        }
    }
}
