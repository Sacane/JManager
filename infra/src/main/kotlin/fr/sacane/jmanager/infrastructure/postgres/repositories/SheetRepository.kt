package fr.sacane.jmanager.infrastructure.postgres.repositories

import fr.sacane.jmanager.infrastructure.postgres.entity.SheetResource
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface SheetRepository : JpaRepository<SheetResource, Long>{
    fun findSheetResourceByIdSheet(id: Long): SheetResource?
}
