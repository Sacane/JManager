package fr.sacane.jmanager.infrastructure.spi.repositories

import fr.sacane.jmanager.infrastructure.spi.entity.SheetResource
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository

@Repository
interface SheetRepository : CrudRepository<SheetResource, Long> {
    fun findSheetResourceByIdSheet(id: Long): SheetResource?
}
