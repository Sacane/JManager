package fr.sacane.jmanager.domain.port.input.user

import fr.sacane.jmanager.domain.hexadoc.DomainService
import fr.sacane.jmanager.domain.models.Booklet
import fr.sacane.jmanager.domain.models.BookletMonthlyCycleSetting
import fr.sacane.jmanager.domain.models.SessionToken
import fr.sacane.jmanager.domain.models.User
import fr.sacane.jmanager.domain.models.UserSettings
import fr.sacane.jmanager.domain.port.spi.SessionManager
import fr.sacane.jmanager.domain.port.spi.UserRepository
import fr.sacane.jmanager.domain.utils.DomainError
import fr.sacane.jmanager.domain.utils.Result
import fr.sacane.jmanager.domain.utils.ResultState
import fr.sacane.jmanager.domain.utils.failure
import fr.sacane.jmanager.domain.utils.success
import java.util.UUID

@DomainService
class GetUserSettingsService(
    private val session: SessionManager,
    private val userRepository: UserRepository
) : GetUserSettingsUseCase {

    override fun getSettings(token: SessionToken): Result<UserSettings> = session.authenticate(token) { userId ->
        val user = userRepository.findUserByIdWithBooklets(userId)
            ?: return@authenticate failure(
                ResultState.USER_NOT_FOUND,
                DomainError(ResultState.USER_NOT_FOUND.code, "domain.user.settings.user_not_found", "L'utilisateur n'existe pas")
            )
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
