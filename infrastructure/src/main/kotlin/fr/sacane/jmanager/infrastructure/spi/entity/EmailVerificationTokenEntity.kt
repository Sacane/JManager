package fr.sacane.jmanager.infrastructure.spi.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.LocalDateTime
import java.util.UUID

@Entity
@Table(name = "email_verification_token")
class EmailVerificationTokenEntity(
    @Id
    @Column(name = "token", nullable = false, length = 64)
    val token: String = "",
    @Column(name = "user_id", nullable = false)
    val userId: UUID? = null,
    @Column(name = "expires_at", nullable = false)
    val expiresAt: LocalDateTime = LocalDateTime.MIN,
)
