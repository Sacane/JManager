package fr.sacane.jmanager.domain.port.input.user

import fr.sacane.jmanager.domain.hexadoc.DomainService
import fr.sacane.jmanager.domain.hexadoc.Port
import fr.sacane.jmanager.domain.hexadoc.Side
import fr.sacane.jmanager.domain.models.BookletMonthlyCycleSetting
import fr.sacane.jmanager.domain.models.UserId
import fr.sacane.jmanager.domain.models.User
import fr.sacane.jmanager.domain.models.UserSettings
import fr.sacane.jmanager.domain.port.output.UserRepository
import fr.sacane.jmanager.domain.port.input.Query
import fr.sacane.jmanager.domain.port.input.QueryHandler
import fr.sacane.jmanager.domain.utils.Result
import fr.sacane.jmanager.domain.utils.ResultState
import fr.sacane.jmanager.domain.utils.DomainError
import fr.sacane.jmanager.domain.utils.failure
import fr.sacane.jmanager.domain.utils.success

data class GetUserSettingsQuery(val userId: UserId) : Query<UserSettings>

@Port(Side.APPLICATION)
interface GetUserSettingsUseCase : QueryHandler<GetUserSettingsQuery, UserSettings> {
    override val queryClass get() = GetUserSettingsQuery::class
}

@DomainService
class GetUserSettingsService(
    private val userRepository: UserRepository
) : GetUserSettingsUseCase {

    override fun handle(query: GetUserSettingsQuery): Result<UserSettings> {
        val userId = query.userId
        val user = userRepository.findUserByIdWithBooklets(userId)
            ?: return failure(
                ResultState.USER_NOT_FOUND,
                DomainError(ResultState.USER_NOT_FOUND.code, "domain.user.settings.user_not_found", "L'utilisateur n'existe pas")
            )
        return success(user.toSettings())
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
