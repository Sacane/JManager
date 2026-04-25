package fr.sacane.jmanager.domain.port.input.user

import fr.sacane.jmanager.domain.hexadoc.Port
import fr.sacane.jmanager.domain.hexadoc.Side
import fr.sacane.jmanager.domain.models.BookletMonthlyCycleUpdate
import fr.sacane.jmanager.domain.models.SessionToken
import fr.sacane.jmanager.domain.models.UserSettings
import fr.sacane.jmanager.domain.utils.Result
import java.util.UUID

@Port(Side.APPLICATION)
interface UpdateUserSettingsUseCase {
    fun updateSettings(
        token: SessionToken,
        projectionWindowDays: Int,
        bookletCycles: Map<UUID, BookletMonthlyCycleUpdate>
    ): Result<UserSettings>
}
