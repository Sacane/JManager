package fr.sacane.jmanager.infrastructure.spi.repositories

import fr.sacane.jmanager.infrastructure.spi.entity.Login
import fr.sacane.jmanager.infrastructure.spi.entity.UserResource
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface LoginRepository: CrudRepository<Login, UUID> {
    fun findByUser(user: UserResource): Login?
}