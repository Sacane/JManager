package fr.sacane.jmanager.infra.server.repositories

import fr.sacane.jmanager.infra.server.entity.AccountResource
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface AccountRepository: JpaRepository<AccountResource, Long>{
}
