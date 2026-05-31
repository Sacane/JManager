package fr.sacane.jmanager.infrastructure.spi.repositories

import fr.sacane.jmanager.infrastructure.spi.entity.EmailVerificationTokenEntity
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface EmailVerificationTokenJpaRepository : CrudRepository<EmailVerificationTokenEntity, String> {
    fun deleteByUserId(userId: UUID)
    fun findByUserId(userId: UUID): List<EmailVerificationTokenEntity>
}
