package fr.sacane.jmanager.domain.port.apiside

import fr.sacane.jmanager.common.hexadoc.PortToLeft
import fr.sacane.jmanager.domain.model.*
import fr.sacane.jmanager.domain.port.serverside.LoginTransactor
import fr.sacane.jmanager.domain.port.serverside.UserTransaction

@PortToLeft
class UserRegisterAdapter(private val port: UserTransaction, private val loginPort: LoginTransactor){

    fun signIn(user: User): Response<User> {
        val userResponse = port.create(user) ?: return Response.emptyInvalidResponse()
        return Response.ok(userResponse)
    }

    fun login(userId: UserId, userPassword: Password, userToken: Token): Response<Ticket> {
        val ticket = loginPort.login(userId, userPassword, userToken)
        return if(ticket.state == TicketState.OK) Response.ok(ticket) else Response.emptyInvalidResponse()
    }

    fun logout(userId: UserId, userToken: Token): Response<Ticket>{
        val ticket = loginPort.logout(userId, userToken)
        return when(ticket.state){
            TicketState.OK -> Response.ok(ticket)
            TicketState.INVALID -> Response.invalid()
            TicketState.TIMEOUT -> Response.timeout()
        }
    }

}