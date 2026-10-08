package fr.sacane.jmanager.infrastructure.spi.repositories

import fr.sacane.jmanager.infrastructure.spi.entity.PasswordResetTokenEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.time.LocalDateTime
import java.util.UUID

@Repository
interface PasswordResetTokenJpaRepository : JpaRepository<PasswordResetTokenEntity, String> {
    fun deleteByUserId(userId: UUID)

    @Modifying
    @Query("DELETE FROM PasswordResetTokenEntity t WHERE t.expiresAt <= :now")
    fun deleteExpired(now: LocalDateTime): Int
}
