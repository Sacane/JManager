package fr.sacane.jmanager.infra.repositories

import fr.sacane.jmanager.infra.entity.SheetResource
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface SheetRepository : JpaRepository<SheetResource, Long>
