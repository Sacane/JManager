package fr.sacane.jmanager.infrastructure.datasource.repositories

import fr.sacane.jmanager.infrastructure.datasource.entity.SheetResource
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface SheetRepository : JpaRepository<SheetResource, Long>
