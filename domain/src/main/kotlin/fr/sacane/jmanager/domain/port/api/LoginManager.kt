package fr.sacane.jmanager.domain.port.api

import fr.sacane.jmanager.domain.hexadoc.LeftPort
import fr.sacane.jmanager.domain.models.*
import fr.sacane.jmanager.domain.port.spi.LoginInventory

@LeftPort
class LoginManager(private val loginPort: LoginInventory){


    fun login(pseudonym: String, userPassword: Password): Response<Ticket> {
        val ticket = loginPort.login(pseudonym, userPassword) ?: return Response.invalid()
        return Response.ok(ticket)
    }

    fun logout(userId: UserId, userToken: Token): Response<Ticket> {
        val ticket = loginPort.logout(userId, userToken) ?: return Response.invalid()
        return Response.ok(ticket)
    }

}