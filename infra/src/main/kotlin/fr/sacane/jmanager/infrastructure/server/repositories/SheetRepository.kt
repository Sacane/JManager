package fr.sacane.jmanager.infrastructure.server.repositories

import fr.sacane.jmanager.infrastructure.server.entity.SheetResource
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface SheetRepository : JpaRepository<SheetResource, Long>
