package fr.sacane.jmanager.server.repositories

import fr.sacane.jmanager.server.entity.SheetResource
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface SheetRepository : JpaRepository<SheetResource, Long>
