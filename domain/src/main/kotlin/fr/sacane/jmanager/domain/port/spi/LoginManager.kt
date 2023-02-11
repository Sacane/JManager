package fr.sacane.jmanager.domain.port.spi

import fr.sacane.jmanager.domain.hexadoc.RightPort
import fr.sacane.jmanager.domain.models.*

@RightPort
interface LoginManager {
    fun login(userPseudonym: String, password: Password): Ticket?
    fun logout(userId: UserId, token: Token): Ticket?
    fun refresh(userId: UserId, token: Token): Ticket?
    fun tokenBy(userId: UserId): Token?
    fun generateToken(user: User): Token?
}