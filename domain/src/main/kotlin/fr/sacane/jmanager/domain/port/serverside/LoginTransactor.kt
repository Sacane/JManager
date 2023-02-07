package fr.sacane.jmanager.domain.port.serverside

import fr.sacane.jmanager.domain.hexadoc.PortToRight
import fr.sacane.jmanager.domain.models.Password
import fr.sacane.jmanager.domain.models.Ticket
import fr.sacane.jmanager.domain.models.Token
import fr.sacane.jmanager.domain.models.UserId

@PortToRight
interface LoginTransactor {
    fun login(userPseudonym: String, password: Password): Ticket?
    fun logout(userId: UserId, token: Token): Ticket?
    fun refresh(userId: UserId, token: Token): Ticket?
}