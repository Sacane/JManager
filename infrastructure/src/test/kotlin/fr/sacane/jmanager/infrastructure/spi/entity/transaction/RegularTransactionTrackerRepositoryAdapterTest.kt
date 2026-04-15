package fr.sacane.jmanager.infrastructure.spi.entity.transaction

import fr.sacane.jmanager.domain.models.transaction.regular.RegularTransactionId
import fr.sacane.jmanager.domain.models.transaction.regular.RegularTransactionTracker
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import java.time.LocalDate
import java.util.UUID

class RegularTransactionTrackerRepositoryAdapterTest {

    @Test
    fun `findTracker returns domain when entity exists`() {
        val jpa = Mockito.mock(JpaRegularTransactionTrackerRepository::class.java)
        val adapter = RegularTransactionTrackerRepositoryAdapter(jpa)

        val regularId = RegularTransactionId("rt-1")
        val bookletId = UUID.randomUUID()
        val entity = RegularTransactionTrackerEntity(
            regularTransactionId = regularId.value,
            bookletId = bookletId,
            lastGeneratedDate = LocalDate.of(2024, 3, 31),
            numberOfGeneratedTransaction = 2,
            id = 10L
        )

        Mockito.`when`(jpa.findByTransactionTrackerByRegularTransactionAndBookletId(regularId.value, bookletId)).thenReturn(entity)

        val result = adapter.findTracker(regularId, bookletId)

        assertNotNull(result)
        assertEquals(10L, result?.id)
        assertEquals(regularId, result?.regularTransactionId)
        assertEquals(bookletId, result?.bookletId)
        assertEquals(2, result?.numberOfGeneratedTransaction)
        assertEquals(LocalDate.of(2024, 3, 31), result?.lastGeneratedDate)
    }

    @Test
    fun `findTracker returns null when not found`() {
        val jpa = Mockito.mock(JpaRegularTransactionTrackerRepository::class.java)
        val adapter = RegularTransactionTrackerRepositoryAdapter(jpa)

        val regularId = RegularTransactionId("rt-absent")
        val bookletId = UUID.randomUUID()

        Mockito.`when`(jpa.findByTransactionTrackerByRegularTransactionAndBookletId(regularId.value, bookletId)).thenReturn(null)

        val result = adapter.findTracker(regularId, bookletId)
        assertNull(result)
    }

    @Test
    fun `upsertTracker saves and returns saved domain`() {
        val jpa = Mockito.mock(JpaRegularTransactionTrackerRepository::class.java)
        val adapter = RegularTransactionTrackerRepositoryAdapter(jpa)

        val regularId = RegularTransactionId("rt-upsert")
        val bookletId = UUID.randomUUID()
        val domain = RegularTransactionTracker(
            id = null,
            regularTransactionId = regularId,
            bookletId = bookletId,
            lastGeneratedDate = LocalDate.of(2024, 5, 10),
            numberOfGeneratedTransaction = 1
        )

        val savedEntity = RegularTransactionTrackerEntity(
            regularTransactionId = regularId.value,
            bookletId = bookletId,
            lastGeneratedDate = LocalDate.of(2024, 5, 10),
            numberOfGeneratedTransaction = 1,
            id = 42L
        )

        Mockito.`when`(jpa.save(Mockito.any(RegularTransactionTrackerEntity::class.java))).thenReturn(savedEntity)

        val result = adapter.upsertTracker(domain)

        assertNotNull(result)
        assertEquals(42L, result.id)
        assertEquals(regularId, result.regularTransactionId)
        assertEquals(bookletId, result.bookletId)
        Mockito.verify(jpa).save(Mockito.any(RegularTransactionTrackerEntity::class.java))
    }

    @Test
    fun `findAllTrackersForBooklet returns mapped domains`() {
        val jpa = Mockito.mock(JpaRegularTransactionTrackerRepository::class.java)
        val adapter = RegularTransactionTrackerRepositoryAdapter(jpa)

        val regularId = RegularTransactionId("rt-list")
        val bookletId = UUID.randomUUID()
        val entity = RegularTransactionTrackerEntity(
            regularTransactionId = regularId.value,
            bookletId = bookletId,
            lastGeneratedDate = LocalDate.of(2024, 1, 1),
            numberOfGeneratedTransaction = 3,
            id = 7L
        )

        Mockito.`when`(jpa.findAllByBookletId(bookletId)).thenReturn(listOf(entity))

        val list = adapter.findAllTrackersForBooklet(bookletId)

        assertEquals(1, list.size)
        val first = list[0]
        assertEquals(7L, first.id)
        assertEquals(regularId, first.regularTransactionId)
        assertEquals(3, first.numberOfGeneratedTransaction)
    }

    @Test
    fun `deleteTrackerByBookletId delegates to jpa repository`() {
        val jpa = Mockito.mock(JpaRegularTransactionTrackerRepository::class.java)
        val adapter = RegularTransactionTrackerRepositoryAdapter(jpa)

        val bookletId = UUID.randomUUID()

        adapter.deleteTrackerByBookletId(bookletId)

        Mockito.verify(jpa).deleteAllByBookletId(bookletId)
    }

    @Test
    fun `deleteTrackerByRegularTransactionId delegates to jpa repository`() {
        val jpa = Mockito.mock(JpaRegularTransactionTrackerRepository::class.java)
        val adapter = RegularTransactionTrackerRepositoryAdapter(jpa)

        val regularId = RegularTransactionId("rt-delete")

        adapter.deleteTrackerByRegularTransactionId(regularId)

        Mockito.verify(jpa).deleteAllByRegularTransactionId(regularId.value)
    }
}

