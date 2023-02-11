package fr.sacane.jmanager.domain.port.api

import fr.sacane.jmanager.domain.models.Response
import fr.sacane.jmanager.domain.models.User
import fr.sacane.jmanager.domain.hexadoc.LeftPort
import fr.sacane.jmanager.domain.port.spi.UserTransaction
//TODO REMOVE IT
@LeftPort
class UserRegister(private val userPort: UserTransaction) {
    fun signIn(user: User): Response<User> {
        val userResponse = userPort.register(user) ?: return Response.invalid()
        return Response.ok(userResponse)
    }
}