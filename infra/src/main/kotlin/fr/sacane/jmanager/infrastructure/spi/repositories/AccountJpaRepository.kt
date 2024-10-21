package fr.sacane.jmanager.infrastructure.spi.repositories

import fr.sacane.jmanager.infrastructure.spi.entity.AccountResource
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.math.BigDecimal

@Repository
interface AccountJpaRepository: CrudRepository<AccountResource, Long>{
    @Query("SELECT acc FROM AccountResource acc LEFT JOIN FETCH acc.sheets sheets WHERE acc.owner.idUser = :userId AND acc.label = :label")
    fun findSheetsByLabelAndAccountOf(label: String, userId: Long): AccountResource?


    @Query("SELECT acc FROM AccountResource acc LEFT JOIN FETCH acc.sheets s LEFT JOIN FETCH s.personalTag LEFT JOIN FETCH s.tag WHERE acc.idAccount = :id")
    fun findByIdWithSheets(id: Long): AccountResource?

    @Query("SELECT account FROM AccountResource account LEFT JOIN FETCH account.sheets WHERE account.owner.idUser = :userId AND account.label = :labelAccount")
    fun findByOwnerAndLabelWithSheets(userId: Long, labelAccount: String): AccountResource?

    @Query("SELECT sheet.position FROM AccountResource account LEFT JOIN account.sheets sheet WHERE account.idAccount = :accountId ORDER BY sheet.position DESC LIMIT 1")
    fun findLastSheetPosition(accountId: Long): Int?

    @Modifying
    @Query("UPDATE AccountResource account SET account.label = :labelAccount, account.amount = :amount, account.previewAmount = :previewAmount WHERE account.idAccount = :id")
    fun update(@Param("labelAccount") labelAccount: String, @Param("amount") amount: BigDecimal, @Param("previewAmount") previewAmount: BigDecimal, @Param("id") id: Long)
}
