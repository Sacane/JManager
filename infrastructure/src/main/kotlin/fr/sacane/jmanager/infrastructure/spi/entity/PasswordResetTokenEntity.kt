package fr.sacane.jmanager.infrastructure.spi.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.LocalDateTime
import java.util.UUID

@Entity
@Table(name = "password_reset_token")
class PasswordResetTokenEntity(
    @Id
    @Column(name = "token_hash", nullable = false, length = 64)
    val tokenHash: String = "",
    @Column(name = "user_id", nullable = false, unique = true)
    val userId: UUID? = null,
    @Column(name = "expires_at", nullable = false)
    val expiresAt: LocalDateTime = LocalDateTime.MIN,
    @Column(name = "created_at", nullable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),
)
