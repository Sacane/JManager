package fr.sacane.jmanager.infra.server.repositories

import fr.sacane.jmanager.infra.server.entity.UserResource
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface UserRepository: JpaRepository<UserResource, Long>
