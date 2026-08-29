package fr.sacane.jmanager.domain.port.input.booklet

import fr.sacane.jmanager.domain.InMemoryDatabase
import fr.sacane.jmanager.domain.fake.InMemoryBookletRepository
import fr.sacane.jmanager.domain.fake.InMemoryUserRepository
import fr.sacane.jmanager.domain.models.Amount
import fr.sacane.jmanager.domain.models.Booklet
import fr.sacane.jmanager.domain.models.SubscriptionPlan
import fr.sacane.jmanager.domain.models.toAmount
import fr.sacane.jmanager.domain.port.output.repository.UnitOfWorkTransactionProvider
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.util.UUID

/**
 * Records whether [executeInTransaction] was ever invoked — used to check that a use case
 * making several repository calls actually goes through the project's transactional boundary
 * pattern (see docs/technical/jpa-transactions/2026-08-29-jpa-fetch-and-transaction-boundary-audit.md,
 * finding D) instead of letting each repository call open its own separate transaction.
 */
private class SpyUnitOfWorkTransactionProvider : UnitOfWorkTransactionProvider {
    var wasCalled: Boolean = false
        private set

    override fun <T, R> executeInTransaction(input: T, executable: (T) -> R): R {
        wasCalled = true
        return executable(input)
    }
}

class BookletTransactionBoundaryTest {

    @Test
    fun `EditBookletService must run its ownership check, read and write inside one transactional boundary`() {
        val db = InMemoryDatabase()
        val userRepository = InMemoryUserRepository(db)
        val bookletRepository = InMemoryBookletRepository(db)
        val user = userRepository.register(
            username = "edit-boundary-user",
            password = "pw",
            roles = emptySet(),
            subscriptionPlan = SubscriptionPlan.FREE,
        )
        val saved = bookletRepository.save(user.id, Booklet(id = UUID.randomUUID(), amount = 10.toAmount(), label = "acct", owner = user))!!

        val spyProvider = SpyUnitOfWorkTransactionProvider()
        val service = EditBookletService(bookletRepository, spyProvider)

        val edited = Booklet(id = saved.id, amount = 20.toAmount(), label = "acct-renamed", owner = user)
        val result = service.handle(EditBookletCommand(edited, user.id))

        assertTrue(result.isSuccess(), "expected the edit to succeed: ${result.message}")
        assertTrue(spyProvider.wasCalled, "EditBookletService must wrap its work in executeInTransaction")
    }

    @Test
    fun `SaveBookletService must check the label and booklet-count limit in the same transactional boundary as the write`() {
        val db = InMemoryDatabase()
        val userRepository = InMemoryUserRepository(db)
        val bookletRepository = InMemoryBookletRepository(db)
        val user = userRepository.register(
            username = "save-boundary-user",
            password = "pw",
            roles = emptySet(),
            subscriptionPlan = SubscriptionPlan.FREE,
        )

        val spyProvider = SpyUnitOfWorkTransactionProvider()
        val service = SaveBookletService(userRepository, bookletRepository, spyProvider)

        val result = service.handle(SaveBookletCommand(user.id, Booklet(id = UUID.randomUUID(), amount = Amount(0L), label = "new-acct", owner = user)))

        assertTrue(result.isSuccess(), "expected the save to succeed: ${result.message}")
        assertTrue(spyProvider.wasCalled, "SaveBookletService must wrap its work in executeInTransaction")
    }
}
