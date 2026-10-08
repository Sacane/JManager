package fr.sacane.jmanager.infrastructure.spi.adapters

import fr.sacane.jmanager.domain.hexadoc.Adapter
import fr.sacane.jmanager.domain.hexadoc.Side
import fr.sacane.jmanager.domain.models.PasswordResetToken
import fr.sacane.jmanager.domain.models.UserId
import fr.sacane.jmanager.domain.port.output.repository.PasswordResetTokenRepository
import fr.sacane.jmanager.infrastructure.spi.entity.PasswordResetTokenEntity
import fr.sacane.jmanager.infrastructure.spi.repositories.PasswordResetTokenJpaRepository
import jakarta.transaction.Transactional
import org.springframework.stereotype.Service
import java.time.LocalDateTime

@Service
@Adapter(Side.INFRASTRUCTURE)
class PasswordResetTokenRepositoryJpaAdapter(
    private val jpaRepository: PasswordResetTokenJpaRepository,
) : PasswordResetTokenRepository {

    // Flushed at once, so a second token for the same user fails here rather than at some later commit.
    override fun save(token: PasswordResetToken): PasswordResetToken {
        jpaRepository.saveAndFlush(token.toEntity())
        return token
    }

    override fun findByTokenHash(tokenHash: String): PasswordResetToken? =
        jpaRepository.findById(tokenHash).orElse(null)?.toDomain()

    @Transactional
    override fun deleteByUserId(userId: UserId) {
        userId.value?.let {
            jpaRepository.deleteByUserId(it)
            jpaRepository.flush()
        }
    }

    @Transactional
    override fun deleteExpired(now: LocalDateTime): Int = jpaRepository.deleteExpired(now)

    private fun PasswordResetToken.toEntity() = PasswordResetTokenEntity(
        tokenHash = tokenHash,
        userId = userId.value,
        expiresAt = expiresAt,
    )

    private fun PasswordResetTokenEntity.toDomain() = PasswordResetToken(
        tokenHash = tokenHash,
        userId = UserId(userId),
        expiresAt = expiresAt,
    )
}
