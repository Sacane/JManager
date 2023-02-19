package fr.sacane.jmanager.infrastructure.server.repositories

import fr.sacane.jmanager.infrastructure.server.entity.AccountResource
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface AccountRepository: JpaRepository<AccountResource, Long>{
    fun findByLabel(label: String): AccountResource?
}
