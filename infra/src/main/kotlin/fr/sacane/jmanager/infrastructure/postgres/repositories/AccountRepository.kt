package fr.sacane.jmanager.infrastructure.postgres.repositories

import fr.sacane.jmanager.infrastructure.postgres.entity.AccountResource
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository

@Repository
interface AccountRepository: CrudRepository<AccountResource, Long>{
    fun findByLabel(label: String): AccountResource?
}
