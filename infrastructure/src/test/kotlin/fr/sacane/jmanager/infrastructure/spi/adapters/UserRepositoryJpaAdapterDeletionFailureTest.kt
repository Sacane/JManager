package fr.sacane.jmanager.infrastructure.spi.adapters

import fr.sacane.jmanager.domain.models.UserId
import fr.sacane.jmanager.domain.utils.ResultState
import fr.sacane.jmanager.infrastructure.spi.repositories.UserPostgresRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.doThrow
import org.mockito.kotlin.mock
import org.springframework.dao.DataIntegrityViolationException
import java.util.UUID

class UserRepositoryJpaAdapterDeletionFailureTest {

    private val userId = UUID.randomUUID()

    // A database refusal must reach the domain as a Result, so the retention purge can move on
    // to the next account instead of aborting the whole run.
    @Test
    fun `deleteById returns an internal failure when the database refuses the deletion`() {
        val repository: UserPostgresRepository = mock {
            on { existsById(userId) } doReturn true
            on { deleteById(userId) } doThrow DataIntegrityViolationException("fk violation")
        }

        val result = UserRepositoryJpaAdapter(repository).deleteById(UserId(userId))

        assertThat(result.isFailure()).isTrue()
        assertThat(result.status).isEqualTo(ResultState.INTERNAL_SERVER_ERROR)
    }
}
