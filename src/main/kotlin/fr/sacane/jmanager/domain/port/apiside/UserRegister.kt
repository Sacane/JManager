package fr.sacane.jmanager.domain.port.apiside

import fr.sacane.jmanager.common.hexadoc.PortToLeft
import fr.sacane.jmanager.domain.model.Password
import fr.sacane.jmanager.domain.model.Response
import fr.sacane.jmanager.domain.model.User
import fr.sacane.jmanager.domain.model.UserId
import fr.sacane.jmanager.domain.port.serverside.UserTransaction

@PortToLeft
class UserRegister(private val userPort: UserTransaction) {
    fun signIn(user: User): Response<User>{
        val userResponse = userPort.save(user) ?: return Response.invalid()
        return Response.ok(userResponse)
    }
}