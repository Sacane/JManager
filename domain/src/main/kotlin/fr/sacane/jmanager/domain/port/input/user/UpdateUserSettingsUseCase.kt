package fr.sacane.jmanager.domain.port.input.user

import fr.sacane.jmanager.domain.hexadoc.DomainService
import fr.sacane.jmanager.domain.hexadoc.Port
import fr.sacane.jmanager.domain.hexadoc.Side
import fr.sacane.jmanager.domain.models.BookletMonthlyCycleUpdate
import fr.sacane.jmanager.domain.models.BookletMonthlyCycleSetting
import fr.sacane.jmanager.domain.models.SessionToken
import fr.sacane.jmanager.domain.models.User
import fr.sacane.jmanager.domain.models.UserSettings
import fr.sacane.jmanager.domain.port.spi.SessionManager
import fr.sacane.jmanager.domain.port.spi.UserRepository
import fr.sacane.jmanager.domain.port.spi.repository.BookletRepository
import fr.sacane.jmanager.domain.port.input.Command
import fr.sacane.jmanager.domain.port.input.CommandHandler
import fr.sacane.jmanager.domain.utils.Result
import fr.sacane.jmanager.domain.utils.ResultState
import fr.sacane.jmanager.domain.utils.DomainError
import fr.sacane.jmanager.domain.utils.failure
import fr.sacane.jmanager.domain.utils.success
import java.util.UUID

data class UpdateUserSettingsCommand(
    val token: SessionToken,
    val projectionWindowDays: Int,
    val bookletCycles: Map<UUID, BookletMonthlyCycleUpdate>
) : Command<UserSettings>

@Port(Side.APPLICATION)
interface UpdateUserSettingsUseCase : CommandHandler<UpdateUserSettingsCommand, UserSettings> {
    override val commandClass get() = UpdateUserSettingsCommand::class
}

@DomainService
class UpdateUserSettingsService(
    private val session: SessionManager,
    private val userRepository: UserRepository,
    private val bookletRepository: BookletRepository
) : UpdateUserSettingsUseCase {

    override fun handle(command: UpdateUserSettingsCommand): Result<UserSettings> = session.authenticate(command.token) { userId ->
        if (command.projectionWindowDays !in 7..60) {
            return@authenticate failure(
                ResultState.INVALID,
                DomainError(ResultState.INVALID.code, "domain.user.settings.invalid_projection_window", "La fenêtre de projection doit être comprise entre 7 et 60 jours")
            )
        }

        val invalidBookletCycle = command.bookletCycles.entries.firstOrNull { (_, cycle) ->
            cycle.monthlyPeriodStartDay !in 1..31
        }
        if (invalidBookletCycle != null) {
            return@authenticate failure(
                ResultState.INVALID,
                DomainError(ResultState.INVALID.code, "domain.user.settings.invalid_monthly_period_start_day", "Le jour de début de période doit être compris entre 1 et 31")
            )
        }

        val invalidBookletCycleEndDay = command.bookletCycles.entries.firstOrNull { (_, cycle) ->
            cycle.monthlyPeriodEndDay != null && cycle.monthlyPeriodEndDay !in 1..31
        }
        if (invalidBookletCycleEndDay != null) {
            return@authenticate failure(
                ResultState.INVALID,
                DomainError(ResultState.INVALID.code, "domain.user.settings.invalid_monthly_period_end_day", "Le jour de fin de période doit être compris entre 1 et 31")
            )
        }

        val user = userRepository.findUserByIdWithBooklets(userId)
            ?: return@authenticate failure(
                ResultState.USER_NOT_FOUND,
                DomainError(ResultState.USER_NOT_FOUND.code, "domain.user.settings.user_not_found", "L'utilisateur n'existe pas")
            )

        val bookletsById = user.booklets.mapNotNull { booklet ->
            booklet.id?.let { id -> id to booklet }
        }.toMap()

        val missingBooklets = bookletsById.keys - command.bookletCycles.keys
        if (missingBooklets.isNotEmpty()) {
            return@authenticate failure(
                ResultState.INVALID,
                DomainError(ResultState.INVALID.code, "domain.user.settings.missing_booklet_cycles", "Chaque livret doit avoir un cycle mensuel configuré")
            )
        }

        for ((bookletId, bookletCycle) in command.bookletCycles) {
            val booklet = bookletsById[bookletId]
                ?: return@authenticate failure(
                    ResultState.FORBIDDEN,
                    DomainError(ResultState.FORBIDDEN.code, "domain.user.settings.booklet_forbidden", "Vous n'avez pas accès au livret $bookletId")
                )

            val updated = bookletRepository.updateMonthlyPeriodStartDay(
                bookletId,
                bookletCycle.monthlyPeriodStartDay,
                bookletCycle.monthlyPeriodEndDay,
            )
            if (!updated) {
                return@authenticate failure(
                    ResultState.INFRASTRUCTURE_ERROR,
                    DomainError(ResultState.INFRASTRUCTURE_ERROR.code, "domain.user.settings.booklet_update_failed", "Impossible de mettre à jour le cycle du livret $bookletId")
                )
            }
            booklet.updateMonthlyPeriodConfiguration(
                bookletCycle.monthlyPeriodStartDay,
                bookletCycle.monthlyPeriodEndDay,
            )
        }

        val projectionUpdated = userRepository.updateProjectionWindowDays(userId, command.projectionWindowDays)
        if (!projectionUpdated) {
            return@authenticate failure(
                ResultState.INFRASTRUCTURE_ERROR,
                DomainError(ResultState.INFRASTRUCTURE_ERROR.code, "domain.user.settings.projection_update_failed", "Impossible de mettre à jour la fenêtre de projection")
            )
        }

        user.updateProjectionWindowDays(command.projectionWindowDays)
        success(user.toSettings())
    }

    private fun User.toSettings(): UserSettings = UserSettings(
        projectionWindowDays = projectionWindowDays,
        bookletCycles = booklets.mapNotNull { booklet ->
            val bookletId = booklet.id ?: return@mapNotNull null
            BookletMonthlyCycleSetting(
                bookletId = bookletId,
                bookletLabel = booklet.label,
                monthlyPeriodStartDay = booklet.monthlyPeriodStartDay,
                monthlyPeriodEndDay = booklet.monthlyPeriodEndDay,
            )
        }
    )
}
