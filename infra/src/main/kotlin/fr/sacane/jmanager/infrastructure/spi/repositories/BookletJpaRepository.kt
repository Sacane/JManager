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
    @Query("SELECT DISTINCT acc FROM BookletResource acc LEFT JOIN FETCH acc.sheets sheets WHERE acc.owner.idUser = :userId AND acc.label = :label")
    fun findSheetsByLabelAndAccountOf(label: String, userId: UUID): BookletResource?


    @Query("""
        SELECT DISTINCT acc FROM BookletResource acc
        LEFT JOIN FETCH acc.sheets s
        LEFT JOIN FETCH s.personalTag
        LEFT JOIN FETCH s.tag
        WHERE acc.idAccount = :id
    """)
    fun findByIdWithSheets(id: UUID): BookletResource?

    @Query("SELECT DISTINCT account FROM BookletResource account LEFT JOIN FETCH account.sheets WHERE account.owner.idUser = :userId AND account.label = :labelAccount")
    fun findByOwnerAndLabelWithSheets(userId: UUID, labelAccount: String): BookletResource?

    @Modifying
    @Query("UPDATE BookletResource account SET account.label = :labelAccount, account.amount = :amount WHERE account.idAccount = :id")
    fun update(@Param("labelAccount") labelAccount: String, @Param("amount") amount: BigDecimal, @Param("id") id: UUID)

    @Modifying
    @Query("UPDATE BookletResource account SET account.monthlyPeriodStartDay = :monthlyPeriodStartDay WHERE account.idAccount = :id")
    fun updateMonthlyPeriodStartDay(@Param("id") id: UUID, @Param("monthlyPeriodStartDay") monthlyPeriodStartDay: Int): Int

    @Query("SELECT DISTINCT account FROM BookletResource account LEFT JOIN FETCH account.sheets WHERE account.idAccount = :id")
    fun findTransactionsById(id: UUID): BookletResource?

    @Query("SELECT account FROM BookletResource account LEFT JOIN FETCH account.regularTransactions WHERE account.idAccount = :id")
    fun findByIdWithRegularTransactions(id: UUID): BookletResource?

    @Query("SELECT DISTINCT account FROM BookletResource account LEFT JOIN FETCH account.sheets LEFT JOIN FETCH account.regularTransactions WHERE account.owner.idUser = :userId")
    fun findAllBookletsByUserId(userId: UUID): List<BookletResource>
}
