package fr.sacane.jmanager.infrastructure.spi.adapters

import fr.sacane.jmanager.domain.models.UserId
import fr.sacane.jmanager.infrastructure.AbstractIntegrationTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.jdbc.core.JdbcTemplate
import java.util.UUID

private val USER_ID: UUID = UUID.fromString("a1e7c0de-0000-4000-8000-000000000001")
private val BOOKLET_ID: UUID = UUID.fromString("a1e7c0de-0000-4000-8000-000000000002")
private val TRANSACTION_ID: UUID = UUID.fromString("a1e7c0de-0000-4000-8000-000000000003")
private val PERSONAL_TAG_ID: UUID = UUID.fromString("a1e7c0de-0000-4000-8000-000000000004")
private val REGULAR_TRANSACTION_ID: UUID = UUID.fromString("a1e7c0de-0000-4000-8000-000000000005")
private const val RECURRENCE_RULE_ID = "a1e7c0de-rule"

/**
 * The privacy policy promises that deleting an account erases its data immediately and irreversibly.
 * This test seeds one row in every table that holds data belonging to a user, deletes the account,
 * and checks that nothing belonging to that user is left behind.
 */
@SpringBootTest
class AccountDeletionErasureIT(
    @Autowired private val userRepositoryJpaAdapter: UserRepositoryJpaAdapter,
    @Autowired private val jdbc: JdbcTemplate,
) : AbstractIntegrationTest() {

    @AfterEach
    fun cleanup() {
        // Leftovers would leak into other tests sharing the container; remove them whatever the outcome.
        leftoverQueries.keys.reversed().forEach { table -> jdbc.update(deleteStatements.getValue(table)) }
    }

    @Test
    fun `shouldEraseEveryRowBelongingToTheUser_whenTheAccountIsDeleted`() {
        seedAFullyUsedAccount()
        assertThat(remainingRowsPerTable().values).allMatch { it > 0 }

        val result = userRepositoryJpaAdapter.deleteById(UserId(USER_ID))

        assertThat(result.isSuccess()).isTrue()
        assertThat(remainingRowsPerTable()).allSatisfy { _, count -> assertThat(count).isZero() }
    }

    private fun remainingRowsPerTable(): Map<String, Int> =
        leftoverQueries.mapValues { (_, query) -> jdbc.queryForObject(query, Int::class.java) ?: 0 }

    private fun seedAFullyUsedAccount() {
        jdbc.update("INSERT INTO user_resource (id_user, username, email, password) VALUES (?, 'erasure-user', 'erasure@example.test', 'hashed')", USER_ID)
        jdbc.update("INSERT INTO user_role (id_user, role) VALUES (?, 'USER')", USER_ID)
        jdbc.update("INSERT INTO booklet (id_booklet, label, owner_id_user, amount, initial_sold) VALUES (?, 'Courant', ?, 100.00, 100.00)", BOOKLET_ID, USER_ID)
        jdbc.update("INSERT INTO tag_personal_resource (id_tag, name, red, green, blue, owner_id_user) VALUES (?, 'Loisirs', 1, 2, 3, ?)", PERSONAL_TAG_ID, USER_ID)
        jdbc.update(
            """
            INSERT INTO transactions (id_transaction, booklet_id_booklet, label_transaction, date, is_income, is_preview,
                                      expenses, booklet_amount, currency, position, personal_tag_id_tag, version)
            VALUES (?, ?, 'Courses', DATE '2026-10-01', false, false, 42.00, 58.00, '€', 0, ?, 0)
            """.trimIndent(),
            TRANSACTION_ID, BOOKLET_ID, PERSONAL_TAG_ID,
        )
        jdbc.update("INSERT INTO tag_personal_resource_linked_transaction (tag_personal_resource_id_tag, linked_transaction_id_transaction) VALUES (?, ?)", PERSONAL_TAG_ID, TRANSACTION_ID)
        jdbc.update("INSERT INTO recurrence_rule (id, type, day_of_month) VALUES (?, 'WEEKLY', NULL)", RECURRENCE_RULE_ID)
        jdbc.update("INSERT INTO recurrence_days_of_week (recurrence_rule_id, day_of_week) VALUES (?, 'MONDAY')", RECURRENCE_RULE_ID)
        jdbc.update(
            """
            INSERT INTO regular_transaction (transaction_id, amount, is_income, label, start_date, owner_id_user, recurrence_rule_id)
            VALUES (?, 9.99, false, 'Abonnement', DATE '2026-01-01', ?, ?)
            """.trimIndent(),
            REGULAR_TRANSACTION_ID, USER_ID, RECURRENCE_RULE_ID,
        )
        jdbc.update("INSERT INTO regular_transaction_booklet (transaction_id, id_booklet) VALUES (?, ?)", REGULAR_TRANSACTION_ID, BOOKLET_ID)
        jdbc.update(
            """
            INSERT INTO regular_transaction_tracker (booklet_id, last_generated_date, regular_transaction_id, version, number_of_generated_transaction)
            VALUES (?, DATE '2026-10-06', ?, 0, 1)
            """.trimIndent(),
            BOOKLET_ID, REGULAR_TRANSACTION_ID.toString(),
        )
        jdbc.update("INSERT INTO regular_recurring_check_entity (month, number_saved_transaction, year, id_user) VALUES (10, 1, 2026, ?)", USER_ID)
        jdbc.update("INSERT INTO email_verification_token (token, user_id, expires_at) VALUES ('erasure-email-token', ?, TIMESTAMP '2026-10-17 09:00:00')", USER_ID)
        jdbc.update("INSERT INTO password_reset_token (token_hash, user_id, expires_at) VALUES (?, ?, TIMESTAMP '2026-10-17 09:00:00')", "e".repeat(64), USER_ID)
    }

    // Ordered parents first; cleanup walks it backwards so children go before their parents.
    private val leftoverQueries: Map<String, String> = linkedMapOf(
        "user_resource" to "SELECT count(*) FROM user_resource WHERE id_user = '$USER_ID'",
        "user_role" to "SELECT count(*) FROM user_role WHERE id_user = '$USER_ID'",
        "booklet" to "SELECT count(*) FROM booklet WHERE id_booklet = '$BOOKLET_ID'",
        "tag_personal_resource" to "SELECT count(*) FROM tag_personal_resource WHERE id_tag = '$PERSONAL_TAG_ID'",
        "recurrence_rule" to "SELECT count(*) FROM recurrence_rule WHERE id = '$RECURRENCE_RULE_ID'",
        "recurrence_days_of_week" to "SELECT count(*) FROM recurrence_days_of_week WHERE recurrence_rule_id = '$RECURRENCE_RULE_ID'",
        "regular_transaction" to "SELECT count(*) FROM regular_transaction WHERE transaction_id = '$REGULAR_TRANSACTION_ID'",
        "regular_transaction_booklet" to "SELECT count(*) FROM regular_transaction_booklet WHERE transaction_id = '$REGULAR_TRANSACTION_ID'",
        "regular_transaction_tracker" to "SELECT count(*) FROM regular_transaction_tracker WHERE booklet_id = '$BOOKLET_ID'",
        "transactions" to "SELECT count(*) FROM transactions WHERE id_transaction = '$TRANSACTION_ID'",
        "tag_personal_resource_linked_transaction" to "SELECT count(*) FROM tag_personal_resource_linked_transaction WHERE linked_transaction_id_transaction = '$TRANSACTION_ID'",
        "regular_recurring_check_entity" to "SELECT count(*) FROM regular_recurring_check_entity WHERE id_user = '$USER_ID'",
        "email_verification_token" to "SELECT count(*) FROM email_verification_token WHERE user_id = '$USER_ID'",
        "password_reset_token" to "SELECT count(*) FROM password_reset_token WHERE user_id = '$USER_ID'",
    )

    private val deleteStatements: Map<String, String> =
        leftoverQueries.mapValues { (_, query) -> query.replace("SELECT count(*) FROM", "DELETE FROM") }
}
