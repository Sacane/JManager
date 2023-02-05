package fr.sacane.jmanager.domain.port.serverside

import fr.sacane.jmanager.common.hexadoc.PortToRight
import fr.sacane.jmanager.domain.model.*

@PortToRight
interface LoginTransactor {
    fun login(userPseudonym: String, password: Password): Ticket?
    fun logout(userId: UserId, token: Token): Ticket?
    fun refresh(userId: UserId, token: Token): Ticket?
}