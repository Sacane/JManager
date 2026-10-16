package fr.sacane.jmanager.infrastructure.spi.adapters

import fr.sacane.jmanager.domain.models.Amount
import fr.sacane.jmanager.domain.models.Booklet
import fr.sacane.jmanager.domain.models.transaction.regular.FrequencyProperty
import fr.sacane.jmanager.domain.models.transaction.regular.RecurrenceRule
import fr.sacane.jmanager.domain.models.transaction.regular.RegularTransaction
import fr.sacane.jmanager.domain.models.transaction.regular.RegularTransactionId
import fr.sacane.jmanager.domain.port.output.repository.TagRepository
import fr.sacane.jmanager.infrastructure.api.AuthenticatedUserTest
import fr.sacane.jmanager.infrastructure.api.setup.BookletStateTestAdapter
import fr.sacane.jmanager.infrastructure.spi.adapters.regular.RegularTransactionRepositoryDataJpaAdapter
import fr.sacane.jmanager.infrastructure.spi.repositories.BookletJpaRepository
import fr.sacane.jmanager.infrastructure.spi.repositories.RegularTransactionResourceJpaRepository
import fr.sacane.jmanager.infrastructure.spi.repositories.UserPostgresRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.test.context.TestPropertySource
import java.time.LocalDate
import java.util.UUID

/**
 * Erasing an account (retention purge, self-deletion, admin deletion) must also remove the
 * account's booklets when a regular transaction is attached to them through the
 * `regular_transaction_booklet` join table.
 */
@TestPropertySource(locations = ["classpath:application-test.properties"])
@SpringBootTest
class UserDeletionWithRegularTransactionIT(
    @Autowired private val userRepositoryJpaAdapter: UserRepositoryJpaAdapter,
    @Autowired private val userPostgresRepository: UserPostgresRepository,
    @Autowired private val bookletJpaRepository: BookletJpaRepository,
    @Autowired private val regularTransactionAdapter: RegularTransactionRepositoryDataJpaAdapter,
    @Autowired private val regularTransactionRepository: RegularTransactionResourceJpaRepository,
    @Autowired private val bookletStateTestAdapter: BookletStateTestAdapter,
    @Autowired private val tagRepository: TagRepository,
    @Autowired private val jdbcTemplate: JdbcTemplate,
) : AuthenticatedUserTest() {

    private lateinit var bookletId: UUID

    @BeforeEach
    fun attachRegularTransactionToBooklet() {
        bookletStateTestAdapter.init(listOf(Booklet(label = "Linked booklet", amount = Amount(1000L), owner = user)))
        bookletId = bookletStateTestAdapter.get().single { it.label == "Linked booklet" }.id!!
        regularTransactionAdapter.saveRegularTransaction(
            user!!.id,
            RegularTransaction(
                label = "Rent",
                amount = Amount(800L),
                isIncome = false,
                tag = tagRepository.defaultTag()!!,
                id = RegularTransactionId(UUID.randomUUID().toString()),
                startDate = LocalDate.of(2026, 1, 1),
                frequencyProperty = FrequencyProperty.Forever(),
                recurrenceRule = RecurrenceRule.Monthly(1),
            ),
            listOf(bookletId),
        )
    }

    @Test
    fun `deleteById removes a user whose booklet is linked to a regular transaction`() {
        val result = userRepositoryJpaAdapter.deleteById(user!!.id)

        assertThat(result.isSuccess()).isTrue()
        assertThat(userPostgresRepository.existsById(user!!.id.value!!)).isFalse()
        assertThat(bookletJpaRepository.existsById(bookletId)).isFalse()
        assertThat(regularTransactionRepository.count()).isZero()
        assertThat(joinRowsOf(bookletId)).isZero()
    }

    private fun joinRowsOf(bookletId: UUID): Int =
        jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM regular_transaction_booklet WHERE id_booklet = ?",
            Int::class.java,
            bookletId,
        )!!
}
