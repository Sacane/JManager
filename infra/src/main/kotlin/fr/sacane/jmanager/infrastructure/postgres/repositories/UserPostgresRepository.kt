package fr.sacane.jmanager.infrastructure.postgres.repositories

import fr.sacane.jmanager.infrastructure.postgres.entity.UserResource
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional

@Repository
interface UserPostgresRepository: CrudRepository<UserResource, Long> {
    fun findByUsername(username: String): UserResource?

    @Transactional
    fun deleteByUsername(username: String)
}
