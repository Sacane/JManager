package fr.sacane.jmanager.domain.port.apiside

import fr.sacane.jmanager.domain.models.Response
import fr.sacane.jmanager.domain.models.User
import fr.sacane.jmanager.domain.hexadoc.PortToLeft
import fr.sacane.jmanager.domain.port.serverside.UserTransaction

@PortToLeft
class UserRegister(private val userPort: UserTransaction) {
    fun signIn(user: User): Response<User> {
        val userResponse = userPort.save(user) ?: return Response.invalid()
        return Response.ok(userResponse)
    }
}