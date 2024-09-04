package fr.sacane.jmanager.infrastructure.spi.repositories

import fr.sacane.jmanager.infrastructure.spi.entity.AccountResource
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository

@Repository
interface AccountJpaRepository: CrudRepository<AccountResource, Long>{
    @Query("SELECT acc FROM AccountResource acc LEFT JOIN FETCH acc.sheets sheets WHERE acc.owner.idUser = :userId AND acc.label = :label")
    fun findSheetsByLabelAndAccountOf(label: String, userId: Long): AccountResource?


    @Query("SELECT acc FROM AccountResource acc LEFT JOIN FETCH acc.sheets WHERE acc.idAccount = :id")
    fun findByIdWithSheets(id: Long): AccountResource?

    @Query("SELECT account FROM AccountResource account LEFT JOIN FETCH account.sheets WHERE account.owner.idUser = :userId AND account.label = :labelAccount")
    fun findByOwnerAndLabelWithSheets(userId: Long, labelAccount: String): AccountResource?
}
