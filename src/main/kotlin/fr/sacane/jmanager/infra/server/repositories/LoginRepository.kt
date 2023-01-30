package fr.sacane.jmanager.infra.server.repositories

import fr.sacane.jmanager.infra.server.entity.Login
import fr.sacane.jmanager.infra.server.entity.UserResource
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.time.LocalDateTime
import java.util.UUID

@Repository
interface LoginRepository: JpaRepository<Login, UUID>{
    fun findByUser(user:UserResource): Login?

    @Modifying
    @Query("update Login l set l.lastRefresh = :lastRefresh, l.token = :token")
    fun refreshAllLogin(lastRefresh: LocalDateTime, token: UUID)

    @Modifying
    @Query("update Login l set l.refreshToken = :refreshToken WHERE l.id = :id")
    fun updateRefresh(refreshToken: UUID, id: UUID)
}