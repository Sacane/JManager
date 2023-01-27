package fr.sacane.jmanager.infra.server.repositories

import fr.sacane.jmanager.infra.server.entity.Login
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface LoginRepository: JpaRepository<Login, UUID>