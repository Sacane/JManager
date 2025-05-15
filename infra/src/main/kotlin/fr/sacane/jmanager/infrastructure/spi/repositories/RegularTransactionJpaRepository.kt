package fr.sacane.jmanager.infrastructure.spi.repositories

import fr.sacane.jmanager.infrastructure.spi.entity.RegularTransactionResource
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface RegularTransactionJpaRepository: JpaRepository<RegularTransactionResource, UUID> {

    @Query("select r from RegularTransactionResource r where r.owner.idUser = :userId")
    fun findAllByUserId(userId: Long): List<RegularTransactionResource>
}