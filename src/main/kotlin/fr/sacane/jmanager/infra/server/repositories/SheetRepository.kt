package fr.sacane.jmanager.infra.server.repositories

import fr.sacane.jmanager.infra.server.entity.SheetResource
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface SheetRepository : JpaRepository<SheetResource, Long>{
    
}
