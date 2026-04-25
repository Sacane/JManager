package fr.sacane.jmanager.domain.port.input.user

import fr.sacane.jmanager.domain.hexadoc.Port
import fr.sacane.jmanager.domain.hexadoc.Side
import fr.sacane.jmanager.domain.models.SessionToken
import fr.sacane.jmanager.domain.utils.Result

@Port(Side.APPLICATION)
interface LogoutUseCase {
    fun logout(token: SessionToken): Result<Nothing>
}
