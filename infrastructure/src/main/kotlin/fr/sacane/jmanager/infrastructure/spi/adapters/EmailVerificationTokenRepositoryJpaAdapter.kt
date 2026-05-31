package fr.sacane.jmanager.infrastructure.spi.adapters

import fr.sacane.jmanager.domain.hexadoc.Adapter
import fr.sacane.jmanager.domain.hexadoc.Side
import fr.sacane.jmanager.domain.models.EmailVerificationToken
import fr.sacane.jmanager.domain.models.UserId
import fr.sacane.jmanager.domain.port.output.repository.EmailVerificationTokenRepository
import fr.sacane.jmanager.infrastructure.spi.entity.EmailVerificationTokenEntity
import fr.sacane.jmanager.infrastructure.spi.repositories.EmailVerificationTokenJpaRepository
import jakarta.transaction.Transactional
import org.springframework.stereotype.Service

@Service
@Adapter(Side.INFRASTRUCTURE)
class EmailVerificationTokenRepositoryJpaAdapter(
    private val jpaRepository: EmailVerificationTokenJpaRepository,
) : EmailVerificationTokenRepository {

    override fun save(token: EmailVerificationToken): EmailVerificationToken {
        jpaRepository.save(token.toEntity())
        return token
    }

    override fun findByToken(token: String): EmailVerificationToken? =
        jpaRepository.findById(token).orElse(null)?.toDomain()

    @Transactional
    override fun deleteByUserId(userId: UserId) {
        userId.value?.let { jpaRepository.deleteByUserId(it) }
    }

    private fun EmailVerificationToken.toEntity() = EmailVerificationTokenEntity(
        token = token,
        userId = userId.value,
        expiresAt = expiresAt,
    )

    private fun EmailVerificationTokenEntity.toDomain() = EmailVerificationToken(
        token = token,
        userId = UserId(userId),
        expiresAt = expiresAt,
    )
}
