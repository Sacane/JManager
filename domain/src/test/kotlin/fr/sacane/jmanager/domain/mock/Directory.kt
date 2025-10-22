package fr.sacane.jmanager.domain.mock

import fr.sacane.jmanager.domain.models.Tag
import fr.sacane.jmanager.domain.models.toAmount
import fr.sacane.jmanager.domain.models.transaction.Transaction
import java.time.LocalDate
import java.time.Month
import java.util.UUID

class Directory {

    companion object{
        val transactionInventories = mutableListOf(
            Transaction(UUID.randomUUID(), "Piano", LocalDate.of(2022, Month.DECEMBER, 1), 450.toAmount(), true, tag = Tag("Fun")),
            Transaction(UUID.randomUUID(), "Salary", LocalDate.now(), 3500.toAmount(), true, tag = Tag("Work")),
            Transaction(UUID.randomUUID(), "SingLessons", LocalDate.now(), 450.toAmount(), true, tag = Tag("Fun")),
            Transaction(UUID.randomUUID(), "Restaurant", LocalDate.of(2022, Month.DECEMBER, 4), 100.toAmount(), true, tag = Tag("Fun")),
            Transaction(UUID.randomUUID(), "Laptop", LocalDate.of(2022, Month.DECEMBER, 31), 450.toAmount(), true, tag = Tag("Nothing")),
            Transaction(UUID.randomUUID(), "", LocalDate.now(), 450.toAmount(), true, tag = Tag("Fun")),
            Transaction(UUID.randomUUID(), "Money From testX", LocalDate.now(), 450.toAmount(), true, tag = Tag("Transaction"))
        )
    }
}
