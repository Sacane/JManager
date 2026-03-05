package fr.sacane.jmanager.domain.models

import fr.sacane.jmanager.domain.models.transaction.Transaction
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.Month
import java.util.UUID

class BookletTest {

    @Test
    fun `Booklet should be created with correct initial values`() {
        val initialAmount = 1000.toAmount()
        val bookletId = UUID.randomUUID()
        val booklet = Booklet(
            amount = initialAmount,
            labelAccount = "Compte principal",
            id = bookletId
        )

        assertEquals(initialAmount, booklet.amount)
        assertEquals("Compte principal", booklet.label)
        assertEquals(initialAmount, booklet.initialSold)
        assertTrue(booklet.transactions.isEmpty())
        assertEquals(bookletId, booklet.id)
    }

    @Test
    fun `addTransaction should update amounts correctly for income transaction`() {
        val booklet = Booklet(1000.toAmount(), "Test")
        val incomeTransaction = Transaction(
            label = "Salaire",
            date = LocalDate.now(),
            amount = 500.toAmount(),
            isIncome = true,
            id = UUID.randomUUID()
        )

        booklet.addTransaction(incomeTransaction)

        assertEquals(1500.toAmount(), booklet.amount)
        assertEquals(1, booklet.transactions.size)
    }

    @Test
    fun `addTransaction should update amounts correctly for expense transaction`() {
        val booklet = Booklet(1000.toAmount(), "Test")
        val expenseTransaction = Transaction(
            id = UUID.randomUUID(),
            label = "Achat",
            date = LocalDate.now(),
            amount = 200.toAmount(),
            isIncome = false
        )

        booklet.addTransaction(expenseTransaction)

        assertEquals(800.toAmount(), booklet.amount)
        assertEquals(1, booklet.transactions.size)
    }

    @Test
    fun `addTransaction with preview should not update real amount`() {
        val booklet = Booklet(1000.toAmount(), "Test")
        val previewTransaction = Transaction(
            id = UUID.randomUUID(),
            label = "Future expense",
            date = LocalDate.now().plusDays(10),
            amount = 100.toAmount(),
            isIncome = false,
            isPreview = true
        )

        booklet.addTransaction(previewTransaction)

        assertEquals(1000.toAmount(), booklet.amount) // Should not change
    }

    @Test
    fun `removeTransactionById should update amounts correctly`() {
        val booklet = Booklet(1000.toAmount(), "Test")
        val transactionId = UUID.randomUUID()
        val transaction = Transaction(
            id = transactionId,
            label = "Test",
            date = LocalDate.now(),
            amount = 200.toAmount(),
            isIncome = true
        )

        booklet.addTransaction(transaction)
        assertEquals(1200.toAmount(), booklet.amount)

        booklet.removeTransactionById(transactionId)
        assertEquals(1000.toAmount(), booklet.amount)
        assertTrue(booklet.transactions.isEmpty())
    }

    @Test
    fun `findTransactionById should return correct transaction`() {
        val booklet = Booklet(1000.toAmount(), "Test")
        val transactionId = UUID.randomUUID()
        val transaction = Transaction(
            id = transactionId,
            label = "Find me",
            date = LocalDate.now(),
            amount = 100.toAmount(),
            isIncome = true
        )

        booklet.addTransaction(transaction)

        val found = booklet.findTransactionById(transactionId)
        assertNotNull(found)
        assertEquals("Find me", found?.label)
        assertEquals(transactionId, found?.id)
    }

    @Test
    fun `findTransactionById should return null for non-existent id`() {
        val booklet = Booklet(1000.toAmount(), "Test")
        val found = booklet.findTransactionById(UUID.randomUUID())
        assertNull(found)
    }

    @Test
    fun `retrieveSheetSurroundAndSortedByDate should filter by month and year`() {
        val booklet = Booklet(1000.toAmount(), "Test")

        booklet.addTransaction(Transaction(UUID.randomUUID(), "Jan transaction", LocalDate.of(2024, 1, 15), 100.toAmount(), true))
        booklet.addTransaction(Transaction(UUID.randomUUID(), "Feb transaction", LocalDate.of(2024, 2, 15), 100.toAmount(), true))
        booklet.addTransaction(Transaction(UUID.randomUUID(), "March transaction", LocalDate.of(2024, 3, 15), 100.toAmount(), true))

        val marchTransactions = booklet.retrieveSheetSurroundAndSortedByDate(Month.MARCH, 2024)

        assertEquals(1, marchTransactions.size)
        assertEquals("March transaction", marchTransactions[0].label)
    }

    @Test
    fun `retrieveSheetSurroundAndSortedByDate should sort transactions by date`() {
        val booklet = Booklet(1000.toAmount(), "Test")

        booklet.addTransaction(Transaction(UUID.randomUUID(), "Later", LocalDate.of(2024, 3, 20), 100.toAmount(), true))
        booklet.addTransaction(Transaction(UUID.randomUUID(), "Earlier", LocalDate.of(2024, 3, 10), 100.toAmount(), true))
        booklet.addTransaction(Transaction(UUID.randomUUID(), "Middle", LocalDate.of(2024, 3, 15), 100.toAmount(), true))

        val transactions = booklet.retrieveSheetSurroundAndSortedByDate(Month.MARCH, 2024)

        assertEquals(3, transactions.size)
        assertEquals("Earlier", transactions[0].label)
        assertEquals("Middle", transactions[1].label)
        assertEquals("Later", transactions[2].label)
    }

    @Test
    fun `retrieveSheetSurroundAndSortedByDate should put preview transactions last`() {
        val booklet = Booklet(1000.toAmount(), "Test")

        booklet.addTransaction(Transaction(UUID.randomUUID(), "Standard", LocalDate.of(2024, 3, 15), 100.toAmount(), true, isPreview = false))
        booklet.addTransaction(Transaction(UUID.randomUUID(), "Preview", LocalDate.of(2024, 3, 10), 100.toAmount(), true, isPreview = true))

        val transactions = booklet.retrieveSheetSurroundAndSortedByDate(Month.MARCH, 2024)

        assertEquals(2, transactions.size)
        assertEquals("Standard", transactions[0].label)
        assertEquals("Preview", transactions[1].label)
    }

    @Test
    fun `removeTransactionIf should remove transactions matching predicate`() {
        val booklet = Booklet(1000.toAmount(), "Test")

        booklet.addTransaction(Transaction(UUID.randomUUID(), "Keep", LocalDate.now(), 100.toAmount(), true))
        booklet.addTransaction(Transaction(UUID.randomUUID(), "Remove1", LocalDate.now(), 50.toAmount(), true))
        booklet.addTransaction(Transaction(UUID.randomUUID(), "Remove2", LocalDate.now(), 50.toAmount(), true))

        booklet.removeTransactionIf { it.amount.value.toInt() == 50 }

        assertEquals(1, booklet.transactions.size)
        assertEquals("Keep", booklet.transactions[0].label)
    }

    @Test
    fun `sheets should return a copy of transactions`() {
        val booklet = Booklet(1000.toAmount(), "Test")
        booklet.addTransaction(Transaction(UUID.randomUUID(), "Transaction", LocalDate.now(), 100.toAmount(), true))

        val sheets = booklet.sheets()

        assertEquals(booklet.transactions.size, sheets.size)
    }

    @Test
    fun `updateFrom should update booklet properties`() {
        val booklet1 = Booklet(1000.toAmount(), "Original")
        val booklet2 = Booklet(2000.toAmount(), "Updated")

        booklet1.updateFrom(booklet2)

        assertEquals(2000.toAmount(), booklet1.amount)
        assertEquals("Updated", booklet1.label)
    }

    @Test
    fun `equals should compare booklets by label`() {
        val booklet1 = Booklet(1000.toAmount(), "Same Label")
        val booklet2 = Booklet(2000.toAmount(), "Same Label")
        val booklet3 = Booklet(1000.toAmount(), "Different Label")

        assertEquals(booklet1, booklet2)
        assertNotEquals(booklet1, booklet3)
    }

    @Test
    fun `hashCode should be based on label`() {
        val booklet1 = Booklet(1000.toAmount(), "Test Label")
        val booklet2 = Booklet(2000.toAmount(), "Test Label")

        assertEquals(booklet1.hashCode(), booklet2.hashCode())
    }

    @Test
    fun `toString should contain booklet information`() {
        val bookletId = UUID.randomUUID()
        val booklet = Booklet(
            amount = 1000.toAmount(),
            labelAccount = "My Account",
            id = bookletId
        )

        val stringRepresentation = booklet.toString()

        assertTrue(stringRepresentation.contains(bookletId.toString()))
        assertTrue(stringRepresentation.contains("1000"))
        assertTrue(stringRepresentation.contains("My Account"))
    }

    @Test
    fun `regularTransactions should return regular transactions list`() {
        val booklet = Booklet(1000.toAmount(), "Test")

        assertTrue(booklet.regularTransactions.isEmpty())
    }
}
