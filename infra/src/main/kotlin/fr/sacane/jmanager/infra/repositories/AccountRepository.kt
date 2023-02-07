package fr.sacane.jmanager.infra.repositories

import fr.sacane.jmanager.infra.entity.AccountResource
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface AccountRepository: JpaRepository<AccountResource, Long>{
}
