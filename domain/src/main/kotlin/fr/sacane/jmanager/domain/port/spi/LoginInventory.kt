package fr.sacane.jmanager.domain.port.spi

import fr.sacane.jmanager.domain.hexadoc.RightPort
import fr.sacane.jmanager.domain.models.Password
import fr.sacane.jmanager.domain.models.Ticket
import fr.sacane.jmanager.domain.models.Token
import fr.sacane.jmanager.domain.models.UserId

@RightPort
interface LoginInventory {
    fun login(userPseudonym: String, password: Password): Ticket?
    fun logout(userId: UserId, token: Token): Ticket?
    fun refresh(userId: UserId, token: Token): Ticket?
}