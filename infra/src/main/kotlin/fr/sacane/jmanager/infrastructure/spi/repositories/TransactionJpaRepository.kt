package fr.sacane.jmanager.infrastructure.spi.repositories

import fr.sacane.jmanager.infrastructure.spi.entity.TransactionResource
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface TransactionJpaRepository : CrudRepository<TransactionResource, UUID> {
    fun findSheetResourceByIdSheet(id: UUID): TransactionResource?
}
