package fr.sacane.jmanager.domain.port.spi

import fr.sacane.jmanager.domain.hexadoc.Side
import fr.sacane.jmanager.domain.hexadoc.Port
import fr.sacane.jmanager.domain.models.*

@Port(Side.DATASOURCE)
interface LoginManager {
    fun login(userPseudonym: String, password: Password): UserToken?
    fun logout(userId: UserId, token: Token): Token?
    fun refresh(userId: UserId, token: Token): UserToken?
    fun tokenBy(userId: UserId): Token?
    fun generateToken(user: User): Token?
}