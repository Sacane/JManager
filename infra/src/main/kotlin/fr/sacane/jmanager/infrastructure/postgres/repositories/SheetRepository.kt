package fr.sacane.jmanager.infrastructure.postgres.repositories

import fr.sacane.jmanager.infrastructure.postgres.entity.SheetResource
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository

@Repository
interface SheetRepository : CrudRepository<SheetResource, Long> {
    fun findSheetResourceByIdSheet(id: Long): SheetResource?
}
