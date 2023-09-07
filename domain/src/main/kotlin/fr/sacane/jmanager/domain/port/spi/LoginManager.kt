package fr.sacane.jmanager.domain.port.spi

import fr.sacane.jmanager.domain.hexadoc.DomainSide
import fr.sacane.jmanager.domain.hexadoc.Port
import fr.sacane.jmanager.domain.models.*

@Port(DomainSide.DATASOURCE)
interface LoginManager {
    fun login(userPseudonym: String, password: Password): Ticket?
    fun logout(userId: UserId, token: Token): Token?
    fun refresh(userId: UserId, token: Token): Ticket?
    fun tokenBy(userId: UserId): Token?
    fun generateToken(user: User): Token?
}