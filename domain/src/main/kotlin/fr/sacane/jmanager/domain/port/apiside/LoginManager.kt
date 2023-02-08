package fr.sacane.jmanager.domain.port.apiside

import fr.sacane.jmanager.domain.hexadoc.PortToLeft
import fr.sacane.jmanager.domain.models.*
import fr.sacane.jmanager.domain.port.serverside.LoginInventory

@PortToLeft
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