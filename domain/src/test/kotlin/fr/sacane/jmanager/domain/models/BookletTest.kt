package fr.sacane.jmanager.domain.models

import fr.sacane.jmanager.domain.models.transaction.Transaction
import fr.sacane.jmanager.domain.models.transaction.regular.FrequencyProperty
import fr.sacane.jmanager.domain.models.transaction.regular.MonthlyRepeatProperty
import fr.sacane.jmanager.domain.models.transaction.regular.MonthlyTransaction
import fr.sacane.jmanager.domain.models.transaction.regular.RegularTransactionId
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.Month

class BookletTest {

    @Test
    fun `Booklet should be created with correct initial values`() {
        val initialAmount = 1000.toAmount()
        val booklet = Booklet(
            amount = initialAmount,
            labelAccount = "Compte principal",
            id = 1L
        )

        assertEquals(initialAmount, booklet.amount)
        assertEquals("Compte principal", booklet.label)
        assertEquals(initialAmount, booklet.initialSold)
        assertEquals(initialAmount, booklet.previewAmount)
        assertTrue(booklet.transactions.isEmpty())
        assertEquals(1L, booklet.id)
        assertNull(booklet.owner)
    }

    @Test
    fun `addTransaction should update amounts correctly for income transaction`() {
        val booklet = Booklet(1000.toAmount(), "Test")
        val incomeTransaction = Transaction(
            id = 1L,
            label = "Salaire",
            date = LocalDate.now(),
            amount = 500.toAmount(),
            isIncome = true
        )

        booklet.addTransaction(incomeTransaction)

        assertEquals(1500.toAmount(), booklet.amount)
        assertEquals(1500.toAmount(), booklet.previewAmount)
        assertEquals(1, booklet.transactions.size)
    }

    @Test
    fun `addTransaction should update amounts correctly for expense transaction`() {
        val booklet = Booklet(1000.toAmount(), "Test")
        val expenseTransaction = Transaction(
            id = 1L,
            label = "Achat",
            date = LocalDate.now(),
            amount = 200.toAmount(),
            isIncome = false
        )

        booklet.addTransaction(expenseTransaction)

        assertEquals(800.toAmount(), booklet.amount)
        assertEquals(800.toAmount(), booklet.previewAmount)
        assertEquals(1, booklet.transactions.size)
    }

    @Test
    fun `addTransaction with preview should only update previewAmount`() {
        val booklet = Booklet(1000.toAmount(), "Test")
        val previewTransaction = Transaction(
            id = 1L,
            label = "Future expense",
            date = LocalDate.now().plusDays(10),
            amount = 100.toAmount(),
            isIncome = false,
            isPreview = true
        )

        booklet.addTransaction(previewTransaction)

        assertEquals(1000.toAmount(), booklet.amount) // Should not change
        assertEquals(900.toAmount(), booklet.previewAmount) // Should change
    }

    @Test
    fun `removeTransactionById should update amounts correctly`() {
        val booklet = Booklet(1000.toAmount(), "Test")
        val transaction = Transaction(
            id = 1L,
            label = "To remove",
            date = LocalDate.now(),
            amount = 200.toAmount(),
            isIncome = true
        )

        booklet.addTransaction(transaction)
        assertEquals(1200.toAmount(), booklet.amount)

        booklet.removeTransactionById(1L)
        assertEquals(1000.toAmount(), booklet.amount)
        assertEquals(1000.toAmount(), booklet.previewAmount)
        assertTrue(booklet.transactions.isEmpty())
    }

    @Test
    fun `findTransactionById should return correct transaction`() {
        val booklet = Booklet(1000.toAmount(), "Test")
        val transaction = Transaction(
            id = 42L,
            label = "Find me",
            date = LocalDate.now(),
            amount = 100.toAmount(),
            isIncome = true
        )

        booklet.addTransaction(transaction)

        val found = booklet.findTransactionById(42L)
        assertNotNull(found)
        assertEquals("Find me", found?.label)
        assertEquals(42L, found?.id)
    }

    @Test
    fun `findTransactionById should return null for non-existent id`() {
        val booklet = Booklet(1000.toAmount(), "Test")
        val found = booklet.findTransactionById(999L)
        assertNull(found)
    }

    @Test
    fun `retrieveSheetSurroundAndSortedByDate should filter by month and year`() {
        val booklet = Booklet(1000.toAmount(), "Test")

        booklet.addTransaction(Transaction(1L, "Jan transaction", LocalDate.of(2024, 1, 15), 100.toAmount(), true))
        booklet.addTransaction(Transaction(2L, "Feb transaction", LocalDate.of(2024, 2, 15), 100.toAmount(), true))
        booklet.addTransaction(Transaction(3L, "March transaction", LocalDate.of(2024, 3, 15), 100.toAmount(), true))

        val marchTransactions = booklet.retrieveSheetSurroundAndSortedByDate(Month.MARCH, 2024)

        assertEquals(1, marchTransactions.size)
        assertEquals("March transaction", marchTransactions[0].label)
    }

    @Test
    fun `retrieveSheetSurroundAndSortedByDate should sort transactions by date`() {
        val booklet = Booklet(1000.toAmount(), "Test")

        booklet.addTransaction(Transaction(1L, "Later", LocalDate.of(2024, 3, 20), 100.toAmount(), true))
        booklet.addTransaction(Transaction(2L, "Earlier", LocalDate.of(2024, 3, 10), 100.toAmount(), true))
        booklet.addTransaction(Transaction(3L, "Middle", LocalDate.of(2024, 3, 15), 100.toAmount(), true))

        val transactions = booklet.retrieveSheetSurroundAndSortedByDate(Month.MARCH, 2024)

        assertEquals(3, transactions.size)
        assertEquals("Earlier", transactions[0].label)
        assertEquals("Middle", transactions[1].label)
        assertEquals("Later", transactions[2].label)
    }

    @Test
    fun `retrieveSheetSurroundAndSortedByDate should put preview transactions last`() {
        val booklet = Booklet(1000.toAmount(), "Test")

        booklet.addTransaction(Transaction(1L, "Standard", LocalDate.of(2024, 3, 15), 100.toAmount(), true, isPreview = false))
        booklet.addTransaction(Transaction(2L, "Preview", LocalDate.of(2024, 3, 10), 100.toAmount(), true, isPreview = true))

        val transactions = booklet.retrieveSheetSurroundAndSortedByDate(Month.MARCH, 2024)

        assertEquals(2, transactions.size)
        assertEquals("Standard", transactions[0].label)
        assertEquals("Preview", transactions[1].label)
    }

    @Test
    fun `removeTransactionIf should remove transactions matching predicate`() {
        val booklet = Booklet(1000.toAmount(), "Test")

        booklet.addTransaction(Transaction(1L, "Keep", LocalDate.now(), 100.toAmount(), true))
        booklet.addTransaction(Transaction(2L, "Remove1", LocalDate.now(), 50.toAmount(), true))
        booklet.addTransaction(Transaction(3L, "Remove2", LocalDate.now(), 50.toAmount(), true))

        booklet.removeTransactionIf { it.amount.value.toInt() == 50 }

        assertEquals(1, booklet.transactions.size)
        assertEquals("Keep", booklet.transactions[0].label)
    }

    @Test
    fun `sheets should return a copy of transactions`() {
        val booklet = Booklet(1000.toAmount(), "Test")
        booklet.addTransaction(Transaction(1L, "Transaction", LocalDate.now(), 100.toAmount(), true))

        val sheets = booklet.sheets()

        assertEquals(booklet.transactions.size, sheets.size)
        assertTrue(sheets is List)
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
        val booklet = Booklet(
            amount = 1000.toAmount(),
            labelAccount = "My Account",
            id = 42L
        )

        val stringRepresentation = booklet.toString()

        assertTrue(stringRepresentation.contains("42"))
        assertTrue(stringRepresentation.contains("1000"))
        assertTrue(stringRepresentation.contains("My Account"))
    }

    @Test
    fun `regularTransactions should return regular transactions list`() {
        val booklet = Booklet(1000.toAmount(), "Test")

        assertTrue(booklet.regularTransactions.isEmpty())
        assertTrue(booklet.regularTransactions is List)
    }
}

