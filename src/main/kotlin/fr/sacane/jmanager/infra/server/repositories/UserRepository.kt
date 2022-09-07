package fr.sacane.jmanager.infra.server.repositories

import fr.sacane.jmanager.infra.server.entity.UserResource
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional

@Repository
interface UserRepository: JpaRepository<UserResource, Long>{
    fun findByPseudonym(pseudo: String): UserResource

    @Transactional
    fun deleteByPseudonym(pseudo: String)
}
