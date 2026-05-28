package fr.sacane.jmanager.domain.port.input.retention

import fr.sacane.jmanager.domain.hexadoc.DomainService
import fr.sacane.jmanager.domain.hexadoc.Port
import fr.sacane.jmanager.domain.hexadoc.Side
import fr.sacane.jmanager.domain.models.retention.PurgeSummary
import fr.sacane.jmanager.domain.models.retention.RetentionPolicy
import fr.sacane.jmanager.domain.port.output.DataRetentionCandidatePort
import fr.sacane.jmanager.domain.port.output.UserRepository
import java.time.LocalDateTime

@Port(Side.APPLICATION)
/**
 * Use case for applying the active [RetentionPolicy] and purging expired personal data.
 *
 * The caller (typically an infrastructure scheduler adapter) is responsible for
 * providing the current timestamp [now]. **Never call `LocalDateTime.now()` inside
 * a domain implementation of this interface.**
 */
interface PurgeExpiredDataUseCase {

    /**
     * Applies [policy] against [now] to find and permanently delete all expired data.
     *
     * @param policy active retention rules
     * @param now    server-side current timestamp, injected by the caller
     * @return [PurgeSummary] describing how many records were deleted
     */
    fun purge(policy: RetentionPolicy, now: LocalDateTime): PurgeSummary
}

@DomainService
class PurgeExpiredDataService(
    private val candidatePort: DataRetentionCandidatePort,
    private val userRepository: UserRepository,
) : PurgeExpiredDataUseCase {

    override fun purge(policy: RetentionPolicy, now: LocalDateTime): PurgeSummary {
        val cutoff = now.minusDays(policy.unconsentedAccountRetentionDays.value)
        val candidates = candidatePort.findUnconsentedUsersCreatedBefore(cutoff)
        val deletedCount = candidates.count { userId ->
            userRepository.deleteById(userId).isSuccess()
        }
        return PurgeSummary(deletedCount)
    }
}
