package fr.sacane.jmanager.infrastructure.spi.repositories

import fr.sacane.jmanager.infrastructure.spi.entity.BookletResource
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.math.BigDecimal
import java.util.UUID

@Repository
interface BookletJpaRepository: CrudRepository<BookletResource, UUID>{
    @Query("SELECT DISTINCT b FROM BookletResource b LEFT JOIN FETCH b.transactions tr WHERE b.owner.idUser = :userId AND b.label = :label")
    fun findTransactionsByLabelForUser(label: String, userId: UUID): BookletResource?


    @Query("""
        SELECT DISTINCT b FROM BookletResource b
        LEFT JOIN FETCH b.transactions t
        LEFT JOIN FETCH t.personalTag
        LEFT JOIN FETCH t.tag
        WHERE b.idBooklet = :id
    """)
    fun findByIdWithTransactions(id: UUID): BookletResource?

    @Query("SELECT DISTINCT b FROM BookletResource b LEFT JOIN FETCH b.transactions WHERE b.owner.idUser = :userId AND b.label = :label")
    fun findByOwnerAndLabelWithTransactions(userId: UUID, @Param("label") label: String): BookletResource?

    @Modifying
    @Query("UPDATE BookletResource b SET b.label = :label, b.amount = :amount WHERE b.idBooklet = :id")
    fun update(@Param("label") bookletLabel: String, @Param("amount") amount: BigDecimal, @Param("id") id: UUID)

    @Modifying
    @Query("UPDATE BookletResource b SET b.monthlyPeriodStartDay = :monthlyPeriodStartDay, b.monthlyPeriodEndDay = :monthlyPeriodEndDay WHERE b.idBooklet = :id")
    fun updateMonthlyPeriodStartDay(@Param("id") id: UUID, @Param("monthlyPeriodStartDay") monthlyPeriodStartDay: Int, @Param("monthlyPeriodEndDay") monthlyPeriodEndDay: Int?): Int

    @Query("SELECT DISTINCT b FROM BookletResource b LEFT JOIN FETCH b.transactions WHERE b.idBooklet = :id")
    fun findTransactionsById(id: UUID): BookletResource?

    @Query("SELECT b FROM BookletResource b LEFT JOIN FETCH b.regularTransactions WHERE b.idBooklet = :id")
    fun findByIdWithRegularTransactions(id: UUID): BookletResource?

    @Query("SELECT DISTINCT b FROM BookletResource b LEFT JOIN FETCH b.transactions LEFT JOIN FETCH b.regularTransactions WHERE b.owner.idUser = :userId")
    fun findAllBookletsByUserId(userId: UUID): List<BookletResource>

    // No fetch join on purpose: this backs ownership checks that run ahead of every mutating
    // booklet/transaction use case. Loading transactions/regularTransactions here (e.g. via
    // findAllBookletsByUserId) is not just wasteful — LEFT JOIN FETCHing two collections on the
    // same booklet produces a cartesian product that duplicates `transactions` bag entries, and a
    // later findByIdWithTransactions call in the same Hibernate session would silently reuse that
    // duplicated collection instead of re-fetching it, causing spurious "not found" failures.
    @Query("SELECT COUNT(b) > 0 FROM BookletResource b WHERE b.idBooklet = :bookletId AND b.owner.idUser = :userId")
    fun existsBookletForUser(bookletId: UUID, userId: UUID): Boolean
}
