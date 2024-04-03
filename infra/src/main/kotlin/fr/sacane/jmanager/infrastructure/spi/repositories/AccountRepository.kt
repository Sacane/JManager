package fr.sacane.jmanager.infrastructure.spi.repositories

import fr.sacane.jmanager.infrastructure.spi.entity.AccountResource
import fr.sacane.jmanager.infrastructure.spi.entity.SheetResource
import fr.sacane.jmanager.infrastructure.spi.entity.UserResource
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository

@Repository
interface AccountRepository: CrudRepository<AccountResource, Long>{
    fun findByLabel(label: String): AccountResource?
    @Query("SELECT acc FROM AccountResource acc LEFT JOIN FETCH acc.sheets sheets WHERE acc.owner.idUser = :userId AND acc.label = :label")
    fun findSheetsByLabelAndAccountOf(label: String, userId: Long): AccountResource?
}
