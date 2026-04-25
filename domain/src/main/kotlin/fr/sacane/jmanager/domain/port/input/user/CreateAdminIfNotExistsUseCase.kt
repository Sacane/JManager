package fr.sacane.jmanager.domain.port.input.user

import fr.sacane.jmanager.domain.hexadoc.Port
import fr.sacane.jmanager.domain.hexadoc.Side
import fr.sacane.jmanager.domain.models.User
import fr.sacane.jmanager.domain.utils.Result

@Port(Side.APPLICATION)
interface CreateAdminIfNotExistsUseCase {
    fun createAdminIfNotExists(username: String, password: String): Result<User>
}
