package fr.sacane.jmanager.infrastructure.spi.adapters

import fr.sacane.jmanager.domain.models.UserId
import fr.sacane.jmanager.domain.utils.ResultState
import fr.sacane.jmanager.infrastructure.spi.repositories.RegularTransactionResourceJpaRepository
import fr.sacane.jmanager.infrastructure.spi.repositories.UserPostgresRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.doThrow
import org.mockito.kotlin.mock
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.transaction.support.TransactionOperations
import java.util.UUID

// A database refusal must reach the domain as a Result, so the retention purge can move on
// to the next account instead of aborting the whole run.
class UserRepositoryJpaAdapterDeletionFailureTest {

    private val userId = UUID.randomUUID()
    private val regularTransactions: RegularTransactionResourceJpaRepository = mock()

    @Test
    fun `deleteById returns an internal failure when the database refuses the deletion`() {
        val users: UserPostgresRepository = mock {
            on { existsById(userId) } doReturn true
            on { deleteById(userId) } doThrow DataIntegrityViolationException("fk violation")
        }

        val result = UserRepositoryJpaAdapter(users, regularTransactions, TransactionOperations.withoutTransaction())
            .deleteById(UserId(userId))

        assertThat(result.isFailure()).isTrue()
        assertThat(result.status).isEqualTo(ResultState.INTERNAL_SERVER_ERROR)
    }

    // Constraint violations surface when the transaction flushes at commit, after the deletes ran.
    @Test
    fun `deleteById returns an internal failure when the transaction fails at commit`() {
        val users: UserPostgresRepository = mock { on { existsById(userId) } doReturn true }
        val failingAtCommit: TransactionOperations = mock {
            on { executeWithoutResult(any()) } doThrow DataIntegrityViolationException("fk violation at commit")
        }

        val result = UserRepositoryJpaAdapter(users, regularTransactions, failingAtCommit).deleteById(UserId(userId))

        assertThat(result.isFailure()).isTrue()
        assertThat(result.status).isEqualTo(ResultState.INTERNAL_SERVER_ERROR)
    }
}
