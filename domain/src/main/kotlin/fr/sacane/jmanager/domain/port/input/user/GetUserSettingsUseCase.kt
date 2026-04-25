package fr.sacane.jmanager.domain.port.input.user

import fr.sacane.jmanager.domain.hexadoc.Port
import fr.sacane.jmanager.domain.hexadoc.Side
import fr.sacane.jmanager.domain.models.UserSettings
import fr.sacane.jmanager.domain.utils.Result

@Port(Side.APPLICATION)
interface GetUserSettingsUseCase {
    fun handle(query: GetUserSettingsQuery): Result<UserSettings>
}
