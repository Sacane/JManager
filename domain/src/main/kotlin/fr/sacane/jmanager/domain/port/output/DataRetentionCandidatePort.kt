package fr.sacane.jmanager.domain.port.output

import fr.sacane.jmanager.domain.hexadoc.Port
import fr.sacane.jmanager.domain.hexadoc.Side
import fr.sacane.jmanager.domain.models.UserId
import java.time.LocalDateTime

@Port(Side.INFRASTRUCTURE)
/**
 * SPI contract for querying user accounts that are candidates for retention purge.
 *
 * Isolates the query responsibility from [UserRepository] so that retention logic
 * can evolve independently of the general user persistence contract.
 */
interface DataRetentionCandidatePort {

    /**
     * Returns the IDs of users who have **never** accepted the Terms of Service
     * (`consent == null`) and whose account was created **strictly before** [cutoff].
     *
     * The strictly-before semantics ensure that an account created exactly at
     * the cutoff instant is not yet eligible — the full retention period must have elapsed.
     *
     * @param cutoff exclusive upper bound on creation date
     * @return list of eligible user IDs; empty list if none found
     */
    fun findUnconsentedUsersCreatedBefore(cutoff: LocalDateTime): List<UserId>
}
