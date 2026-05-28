package fr.sacane.jmanager.domain.port.input.user

import fr.sacane.jmanager.domain.hexadoc.DomainService
import fr.sacane.jmanager.domain.hexadoc.Port
import fr.sacane.jmanager.domain.hexadoc.Side
import fr.sacane.jmanager.domain.models.ConsentRecord
import fr.sacane.jmanager.domain.models.UserId
import fr.sacane.jmanager.domain.port.input.Command
import fr.sacane.jmanager.domain.port.input.CommandHandler
import fr.sacane.jmanager.domain.port.output.UserRepository
import fr.sacane.jmanager.domain.utils.DomainError
import fr.sacane.jmanager.domain.utils.Result
import fr.sacane.jmanager.domain.utils.ResultState
import fr.sacane.jmanager.domain.utils.failure
import org.slf4j.LoggerFactory
import java.time.LocalDateTime

/**
 * Command carrying the user's explicit consent to TOS and Privacy Policy.
 * The [consentTimestamp] is always generated server-side by the application layer —
 * the domain never calls LocalDateTime.now() directly.
 */
data class RecordConsentCommand(
    val userId: UserId,
    val tosAccepted: Boolean,
    val tosVersion: String?,
    val privacyAccepted: Boolean,
    val consentTimestamp: LocalDateTime,
) : Command<Unit>

@Port(Side.APPLICATION)
interface RecordConsentUseCase : CommandHandler<RecordConsentCommand, Unit> {
    override val commandClass get() = RecordConsentCommand::class
}

@DomainService
class RecordConsentService(
    private val userRepository: UserRepository,
) : RecordConsentUseCase {

    companion object {
        private val log = LoggerFactory.getLogger(RecordConsentService::class.java)
    }

    override fun handle(command: RecordConsentCommand): Result<Unit> {
        if (!command.tosAccepted) {
            return failure(
                ResultState.INVALID,
                DomainError(ResultState.INVALID.code, "domain.user.consent.tos_required", "L'acceptation des CGU est obligatoire"),
            )
        }
        if (!command.privacyAccepted) {
            return failure(
                ResultState.INVALID,
                DomainError(ResultState.INVALID.code, "domain.user.consent.privacy_required", "L'acceptation de la politique de confidentialité est obligatoire"),
            )
        }
        userRepository.findUserById(command.userId)
            ?: return failure(
                ResultState.USER_NOT_FOUND,
                DomainError(ResultState.USER_NOT_FOUND.code, "domain.user.consent.user_not_found", "L'utilisateur est introuvable"),
            )

        val consent = ConsentRecord(
            tosAcceptedAt = command.consentTimestamp,
            tosVersion = command.tosVersion,
            privacyAcceptedAt = command.consentTimestamp,
        )

        return userRepository.recordConsent(command.userId, consent).also {
            if (it.isSuccess()) log.info("Consent recorded successfully")
        }
    }
}
