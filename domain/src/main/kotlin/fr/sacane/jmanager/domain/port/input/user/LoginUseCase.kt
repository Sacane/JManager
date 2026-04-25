package fr.sacane.jmanager.domain.port.input.user

import fr.sacane.jmanager.domain.hexadoc.Port
import fr.sacane.jmanager.domain.hexadoc.Side
import fr.sacane.jmanager.domain.models.UserToken
import fr.sacane.jmanager.domain.utils.Result

@Port(Side.APPLICATION)
interface LoginUseCase {
    fun handle(command: LoginCommand): Result<UserToken>
}
