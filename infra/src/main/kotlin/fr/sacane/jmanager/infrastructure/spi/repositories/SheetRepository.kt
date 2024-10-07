package fr.sacane.jmanager.infrastructure.spi.repositories

import fr.sacane.jmanager.infrastructure.spi.entity.TransactionResource
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository

@Repository
interface SheetRepository : CrudRepository<TransactionResource, Long> {
    fun findSheetResourceByIdSheet(id: Long): TransactionResource?
}
