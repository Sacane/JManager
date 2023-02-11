package fr.sacane.jmanager.infrastructure.server.repositories

import fr.sacane.jmanager.infrastructure.server.entity.Login
import fr.sacane.jmanager.infrastructure.server.entity.UserResource
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface LoginRepository: JpaRepository<Login, UUID>{
    fun findByUser(user: UserResource): Login?
}