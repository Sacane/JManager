package fr.sacane.jmanager.infrastructure.spi.entity.transaction

import fr.sacane.jmanager.domain.models.transaction.regular.RegularTransactionId
import fr.sacane.jmanager.domain.models.transaction.regular.RegularTransactionTracker
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.util.UUID

class RegularTransactionTrackerEntityTest {

    @Test
    fun `instantiation and toDomain should map fields correctly`() {
        val bookletId = UUID.randomUUID()
        val now = LocalDate.now()

        val entity = RegularTransactionTrackerEntity(
            regularTransactionId = "regular-123",
            bookletId = bookletId,
            lastGeneratedDate = now,
            numberOfGeneratedTransaction = 5,
            id = 42L
        )

        val domain = entity.toDomain()

        assertEquals(42L, domain.id)
        assertEquals("regular-123", domain.regularTransactionId.value)
        assertEquals(bookletId, domain.bookletId)
        assertEquals(now, domain.lastGeneratedDate)
        assertEquals(5, domain.numberOfGeneratedTransaction)
    }

    @Test
    fun `fromDomain should create equivalent entity`() {
        val bookletId = UUID.randomUUID()
        val now = LocalDate.now()

        val domain = RegularTransactionTracker(
            id = 7L,
            regularTransactionId = RegularTransactionId("reg-7"),
            bookletId = bookletId,
            lastGeneratedDate = now,
            numberOfGeneratedTransaction = 2
        )

        val entity = RegularTransactionTrackerEntity.fromDomain(domain)

        assertEquals(7L, entity.id)
        assertEquals("reg-7", entity.regularTransactionId)
        assertEquals(bookletId, entity.bookletId)
        assertEquals(now, entity.lastGeneratedDate)
        assertEquals(2, entity.numberOfGeneratedTransaction)
    }

    @Test
    fun `equals and hashCode should be consistent for identical instances`() {
        val bookletId = UUID.randomUUID()
        val now = LocalDate.now()

        val a = RegularTransactionTrackerEntity(
            regularTransactionId = "r1",
            bookletId = bookletId,
            lastGeneratedDate = now,
            numberOfGeneratedTransaction = 0,
            id = 1L
        )

        val b = RegularTransactionTrackerEntity(
            regularTransactionId = "r1",
            bookletId = bookletId,
            lastGeneratedDate = now,
            numberOfGeneratedTransaction = 0,
            id = 1L
        )

        val c = RegularTransactionTrackerEntity(
            regularTransactionId = "r2",
            bookletId = bookletId,
            lastGeneratedDate = now,
            numberOfGeneratedTransaction = 0,
            id = 2L
        )

        assertEquals(a, b)
        assertEquals(a.hashCode(), b.hashCode())
        assertNotEquals(a, c)
    }
}
