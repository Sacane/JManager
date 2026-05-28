package fr.sacane.jmanager.infrastructure.spi.adapters.retention

import fr.sacane.jmanager.domain.hexadoc.Adapter
import fr.sacane.jmanager.domain.hexadoc.Side
import fr.sacane.jmanager.domain.models.UserId
import fr.sacane.jmanager.domain.port.output.DataRetentionCandidatePort
import fr.sacane.jmanager.infrastructure.spi.repositories.UserPostgresRepository
import org.springframework.stereotype.Service
import java.time.LocalDateTime

@Service
@Adapter(Side.INFRASTRUCTURE)
class DataRetentionCandidateJpaAdapter(
    private val userPostgresRepository: UserPostgresRepository,
) : DataRetentionCandidatePort {

    override fun findUnconsentedUsersCreatedBefore(cutoff: LocalDateTime): List<UserId> =
        userPostgresRepository
            .findIdsByConsentNullAndCreationDateBefore(cutoff)
            .map { UserId(it) }
}
