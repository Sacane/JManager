package fr.sacane.jmanager.infrastructure.spi.adapters.transaction

import fr.sacane.jmanager.domain.models.Amount
import fr.sacane.jmanager.domain.models.Booklet
import fr.sacane.jmanager.domain.models.Tag
import fr.sacane.jmanager.domain.models.transaction.Transaction
import fr.sacane.jmanager.domain.models.transaction.regular.FrequencyProperty
import fr.sacane.jmanager.domain.models.transaction.regular.RecurrenceRule
import fr.sacane.jmanager.domain.models.transaction.regular.RegularTransaction
import fr.sacane.jmanager.domain.models.transaction.regular.RegularTransactionId
import fr.sacane.jmanager.domain.port.input.transaction.DeleteTransactionsByIdsCommand
import fr.sacane.jmanager.domain.port.input.transaction.DeleteTransactionsByIdsUseCase
import fr.sacane.jmanager.domain.port.output.repository.TagRepository
import fr.sacane.jmanager.domain.utils.ResultState
import fr.sacane.jmanager.infrastructure.api.AuthenticatedUserTest
import fr.sacane.jmanager.infrastructure.api.setup.BookletStateTestAdapter
import fr.sacane.jmanager.infrastructure.spi.adapters.regular.RegularTransactionRepositoryDataJpaAdapter
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
 * Regression test for a confirmed production bug — see
 * docs/bugs/preview-transaction-race-duplicate/REPORT.md.
 *
 * `userOwnsBooklet` used to call `BookletRepository.findBookletsForUser`, whose query
 * (`findAllBookletsByUserId`) did `LEFT JOIN FETCH b.transactions LEFT JOIN FETCH
 * b.regularTransactions` in one shot. `transactions` is a plain `List` (bag, `@OneToMany` with no
 * `@OrderColumn`) while `regularTransactions` is a `Set`. Fetching a bag alongside another
 * collection in the same JPQL query is a well-known Hibernate cartesian-product trap: each bag
 * element gets duplicated once per matching row in the OTHER collection, because JPQL `DISTINCT`
 * only dedupes the root entity, not bag elements. With N regular transactions linked to the
 * booklet, every transaction of that booklet — including one with no `regularTransactionId` at
 * all, since the duplication happens at the booklet level — ended up duplicated N times in
 * `booklet.transactions`. Because `findBookletByIdWithTransactions` then ran in the SAME Hibernate
 * session (`executeInTransaction` is `@Transactional`, both adapter methods are too), Hibernate's
 * session identity map returned the SAME managed entity instead of re-fetching, reusing that
 * already-initialized (duplicated) list. `DeleteTransactionsByIdsService` then did
 * `booklet.transactions.filter { ids.contains(it.id) }`, requiring the result size to equal the
 * number of requested ids — with N > 1 duplicates that equality failed even though the transaction
 * was genuinely present, misreporting TRANSACTION_NOT_FOUND.
 *
 * Fixed by replacing `userOwnsBooklet`'s membership check with
 * `BookletRepository.existsBookletForUser`, a fetch-join-free existence query.
 */
@TestPropertySource(locations = ["classpath:application-test.properties"])
@SpringBootTest
class DeleteTransactionsByIdsUseCaseOwnershipSideEffectTest(
    @Autowired private val deleteTransactionsByIdsUseCase: DeleteTransactionsByIdsUseCase,
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
    fun `deleting a preview transaction should succeed even when the booklet has several regular transactions linked`() {
        val booklet = Booklet(label = "acct-preview-delete-multi", amount = Amount(100L), owner = user)
        bookletStateTestAdapter.init(listOf(booklet))
        val persistedBooklet = transactionRepositoryJpaAdapter.findBookletByLabelWithTransactions(booklet.label, user!!.id)
        val bookletId = persistedBooklet!!.id!!

        val defaultTag: Tag = tagRepository.defaultTag()!!

        // Link THREE regular transactions to the booklet — this is what varies from the previous
        // (passing) attempt, which only had one.
        val regularTransactions = (1..3).map { n ->
            RegularTransaction(
                label = "regular-$n",
                amount = Amount(100L * n),
                isIncome = false,
                tag = defaultTag,
                id = RegularTransactionId(UUID.randomUUID().toString()),
                startDate = LocalDate.of(2024, 1, 1),
                frequencyProperty = FrequencyProperty.Forever(),
                recurrenceRule = RecurrenceRule.Monthly(1),
            )
        }
        regularTransactions.forEach {
            regularTransactionAdapter.saveRegularTransaction(user!!.id, it, listOf(bookletId))
        }

        // Persist a real, physical preview transaction created manually — NOT tied to any
        // regular transaction (regularTransactionId = null). This is the user's actual scenario.
        // The cartesian duplication from userOwnsBooklet's query happens at the booklet level
        // (transactions x regularTransactions linked to the SAME booklet), so it should affect
        // this transaction too even though it isn't itself linked to a regular transaction.
        val previewTx = Transaction(
            id = null,
            label = "manual preview",
            date = LocalDate.now(),
            amount = Amount(100L),
            isIncome = false,
            isPreview = true,
            tag = defaultTag,
            regularTransactionId = null,
        )
        val saved = transactionRepositoryJpaAdapter.save(bookletId, previewTx)
        val savedId = saved!!.id!!

        // Act — mirrors DeleteTransactionsByIdsService.handle exactly: userOwnsBooklet() first
        // (which fetch-joins transactions AND regularTransactions for every booklet the user
        // owns), then findBookletByIdWithTransactions(), both inside the same transaction.
        val result = deleteTransactionsByIdsUseCase.handle(
            DeleteTransactionsByIdsCommand(user!!.id, bookletId, listOf(savedId))
        )

        assertThat(result.status)
            .describedAs("delete result: %s", result.message)
            .isEqualTo(ResultState.OK)
    }
}
