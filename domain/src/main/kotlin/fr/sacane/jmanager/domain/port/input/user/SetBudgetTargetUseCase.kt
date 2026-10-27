package fr.sacane.jmanager.domain.port.input.user

import fr.sacane.jmanager.domain.hexadoc.DomainService
import fr.sacane.jmanager.domain.hexadoc.Port
import fr.sacane.jmanager.domain.hexadoc.Side
import fr.sacane.jmanager.domain.models.UserId
import fr.sacane.jmanager.domain.port.input.Command
import fr.sacane.jmanager.domain.port.input.CommandHandler
import fr.sacane.jmanager.domain.port.output.repository.BookletRepository
import fr.sacane.jmanager.domain.utils.DomainError
import fr.sacane.jmanager.domain.utils.Result
import fr.sacane.jmanager.domain.utils.ResultState
import fr.sacane.jmanager.domain.utils.failure
import fr.sacane.jmanager.domain.utils.success
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.UUID

/** Sets the monthly budget target of one booklet of the user; a null or zero [amount] removes it. */
data class SetBudgetTargetCommand(val userId: UserId, val bookletId: UUID, val amount: BigDecimal?) : Command<Unit>

@Port(Side.APPLICATION)
interface SetBudgetTargetUseCase : CommandHandler<SetBudgetTargetCommand, Unit> {
    override val commandClass get() = SetBudgetTargetCommand::class
}

@DomainService
class SetBudgetTargetService(
    private val bookletRepository: BookletRepository,
) : SetBudgetTargetUseCase {

    companion object {
        /** The largest amount a NUMERIC(12, 2) column holds. */
        val MAX_BUDGET_TARGET = BigDecimal("9999999999.99")
    }

    override fun handle(command: SetBudgetTargetCommand): Result<Unit> {
        val amount = command.amount?.setScale(2, RoundingMode.HALF_UP)
        if (amount != null && amount.signum() < 0) {
            return failure(
                ResultState.INVALID,
                DomainError(ResultState.INVALID.code, "domain.user.settings.negative_budget_target", "Le budget ne peut pas être négatif"),
            )
        }
        if (amount != null && amount > MAX_BUDGET_TARGET) {
            return failure(
                ResultState.INVALID,
                DomainError(ResultState.INVALID.code, "domain.user.settings.budget_target_too_large", "Ce budget est trop élevé"),
            )
        }
        // An unknown booklet answers like someone else's: whether it exists is none of the caller's business.
        if (!bookletRepository.existsBookletForUser(command.userId, command.bookletId)) {
            return failure(
                ResultState.FORBIDDEN,
                DomainError(ResultState.FORBIDDEN.code, "domain.user.settings.booklet_forbidden", "Vous n'avez pas accès au livret ${command.bookletId}"),
            )
        }

        val target = amount?.takeIf { it.signum() > 0 }
        if (!bookletRepository.updateBudgetTarget(command.bookletId, target)) {
            return failure(
                ResultState.INFRASTRUCTURE_ERROR,
                DomainError(ResultState.INFRASTRUCTURE_ERROR.code, "domain.user.settings.budget_target_update_failed", "Impossible d'enregistrer le budget"),
            )
        }
        return success(Unit)
    }
}
