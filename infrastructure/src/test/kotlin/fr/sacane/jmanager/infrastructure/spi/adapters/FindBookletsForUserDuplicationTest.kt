package fr.sacane.jmanager.infrastructure.spi.adapters

import fr.sacane.jmanager.domain.models.Amount
import fr.sacane.jmanager.domain.models.Booklet
import fr.sacane.jmanager.domain.models.Tag
import fr.sacane.jmanager.domain.models.transaction.Transaction
import fr.sacane.jmanager.domain.models.transaction.regular.FrequencyProperty
import fr.sacane.jmanager.domain.models.transaction.regular.RecurrenceRule
import fr.sacane.jmanager.domain.models.transaction.regular.RegularTransaction
import fr.sacane.jmanager.domain.models.transaction.regular.RegularTransactionId
import fr.sacane.jmanager.domain.port.output.repository.TagRepository
import fr.sacane.jmanager.infrastructure.api.AuthenticatedUserTest
import fr.sacane.jmanager.infrastructure.api.setup.BookletStateTestAdapter
import fr.sacane.jmanager.infrastructure.spi.adapters.regular.RegularTransactionRepositoryDataJpaAdapter
import fr.sacane.jmanager.infrastructure.spi.adapters.transaction.TransactionRepositoryJpaAdapter
import fr.sacane.jmanager.infrastructure.spi.entity.transaction.JpaRegularTransactionTrackerRepository
import fr.sacane.jmanager.infrastructure.spi.repositories.RegularTransactionResourceJpaRepository
import fr.sacane.jmanager.infrastructure.spi.repositories.TransactionJpaRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.TestPropertySource
import java.time.LocalDate
import java.util.UUID

/**
 * Confirms that `findBookletsForUser` (still used by the stats read path — see
 * StatsDomainHelper.withScopedBooklets, used when no bookletId is given, i.e. the "Tous les
 * comptes" dashboard view) returns DUPLICATED transaction entries when a booklet has 2+ regular
 * transactions linked — the same cartesian-fetch mechanism documented in
 * docs/bugs/preview-transaction-race-duplicate/REPORT.md and fixed for the ownership-check call
 * site (userOwnsBooklet), but NOT fixed here: this method still backs real financial aggregation
 * (category distribution, trend stats, previsional transactions), so the duplication silently
 * inflates the numbers shown to the user instead of raising an error.
 *
 * See docs/technical/jpa-transactions/2026-08-29-jpa-fetch-and-transaction-boundary-audit.md
 * (finding A) — fixed by dropping the `regularTransactions` fetch join from
 * `findAllBookletsByUserId`: nothing downstream of `withScopedBooklets` ever reads
 * `Booklet.regularTransactions`, so the query only needs `transactions`.
 */
@TestPropertySource(locations = ["classpath:application-test.properties"])
@SpringBootTest
class FindBookletsForUserDuplicationTest(
    @Autowired private val bookletJpaRepositoryAdapter: BookletJpaRepositoryAdapter,
    @Autowired private val transactionRepositoryJpaAdapter: TransactionRepositoryJpaAdapter,
    @Autowired private val regularTransactionAdapter: RegularTransactionRepositoryDataJpaAdapter,
    @Autowired private val bookletStateTestAdapter: BookletStateTestAdapter,
    @Autowired private val tagRepository: TagRepository,
    @Autowired private val transactionJpaRepository: TransactionJpaRepository,
    @Autowired private val regularTransactionJpaRepository: RegularTransactionResourceJpaRepository,
    @Autowired private val trackerRepository: JpaRegularTransactionTrackerRepository,
) : AuthenticatedUserTest() {

    @AfterEach
    fun clear() {
        trackerRepository.deleteAll()
        transactionJpaRepository.deleteAll()
        regularTransactionJpaRepository.deleteAll()
        bookletStateTestAdapter.clear()
    }

    @Test
    fun `findBookletsForUser must not duplicate transactions when the booklet has several regular transactions linked`() {
        val booklet = Booklet(label = "acct-stats-dup", amount = Amount(100L), owner = user)
        bookletStateTestAdapter.init(listOf(booklet))
        val persistedBooklet = transactionRepositoryJpaAdapter.findBookletByLabelWithTransactions(booklet.label, user!!.id)
        val bookletId = persistedBooklet!!.id!!
        val defaultTag: Tag = tagRepository.defaultTag()!!

        // 3 regular transactions linked to the booklet — this is what drives the cartesian
        // multiplication in findAllBookletsByUserId's LEFT JOIN FETCH b.transactions LEFT JOIN
        // FETCH b.regularTransactions.
        (1..3).forEach { n ->
            val regular = RegularTransaction(
                label = "regular-$n",
                amount = Amount(100L * n),
                isIncome = false,
                tag = defaultTag,
                id = RegularTransactionId(UUID.randomUUID().toString()),
                startDate = LocalDate.of(2024, 1, 1),
                frequencyProperty = FrequencyProperty.Forever(),
                recurrenceRule = RecurrenceRule.Monthly(1),
            )
            regularTransactionAdapter.saveRegularTransaction(user!!.id, regular, listOf(bookletId))
        }

        // A single, ordinary physical transaction — should appear exactly once.
        val tx = Transaction(
            id = null,
            label = "groceries",
            date = LocalDate.now(),
            amount = Amount(50L),
            isIncome = false,
            isPreview = false,
            tag = defaultTag,
            regularTransactionId = null,
        )
        transactionRepositoryJpaAdapter.save(bookletId, tx)

        val booklets = bookletJpaRepositoryAdapter.findBookletsForUser(user!!.id)
        val loadedBooklet = booklets.first { it.id == bookletId }

        // This is the assertion that fails today: the single transaction is duplicated once per
        // linked regular transaction (3), so size is 3 instead of 1 — which is exactly what
        // silently inflates category distribution / trend stats totals on the "Tous les comptes"
        // dashboard view for any account with 2+ regular transactions.
        assertThat(loadedBooklet.transactions)
            .describedAs("transactions returned by findBookletsForUser for booklet %s", bookletId)
            .hasSize(1)
    }
}
