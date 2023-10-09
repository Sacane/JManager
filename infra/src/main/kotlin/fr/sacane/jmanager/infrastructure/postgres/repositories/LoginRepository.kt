package fr.sacane.jmanager.infrastructure.postgres.repositories

import fr.sacane.jmanager.infrastructure.postgres.entity.Login
import fr.sacane.jmanager.infrastructure.postgres.entity.UserResource
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface LoginRepository: CrudRepository<Login, UUID> {
    fun findByUser(user: UserResource): Login?
}