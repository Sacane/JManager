package fr.sacane.jmanager.domain.mock

import fr.sacane.jmanager.domain.models.Tag
import fr.sacane.jmanager.domain.models.toAmount
import fr.sacane.jmanager.domain.models.transaction.Transaction
import java.time.LocalDate
import java.time.Month

class Directory {

    companion object{
        val transactionInventories = mutableListOf(
            Transaction(0, "Piano", LocalDate.of(2022, Month.DECEMBER, 1), 450.toAmount(), true, tag = Tag("Fun")),
            Transaction(1, "Salary", LocalDate.now(), 3500.toAmount(), true, tag = Tag("Work")),
            Transaction(2, "SingLessons", LocalDate.now(), 450.toAmount(), true, tag = Tag("Fun")),
            Transaction(3, "Restaurant", LocalDate.of(2022, Month.DECEMBER, 4), 100.toAmount(), true, tag = Tag("Fun")),
            Transaction(4, "Laptop", LocalDate.of(2022, Month.DECEMBER, 31), 450.toAmount(), true, tag = Tag("Nothing")),
            Transaction(5, "", LocalDate.now(), 450.toAmount(), true, tag = Tag("Fun")),
            Transaction(6, "Money From testX", LocalDate.now(), 450.toAmount(), true, tag = Tag("Transaction"))
        )
    }
}
