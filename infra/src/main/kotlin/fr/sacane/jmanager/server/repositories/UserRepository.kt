package fr.sacane.jmanager.server.repositories

import fr.sacane.jmanager.server.entity.UserResource
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional

@Repository
interface UserRepository: JpaRepository<UserResource, Long>{
    fun findByPseudonym(pseudo: String): UserResource?

    @Transactional
    fun deleteByPseudonym(pseudo: String)
}
