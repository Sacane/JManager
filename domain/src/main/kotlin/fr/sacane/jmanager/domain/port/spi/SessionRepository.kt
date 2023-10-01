package fr.sacane.jmanager.domain.port.spi

import fr.sacane.jmanager.domain.hexadoc.Side
import fr.sacane.jmanager.domain.hexadoc.Port
import fr.sacane.jmanager.domain.models.*

@Port(Side.DATASOURCE)
interface SessionRepository {
    fun login(userPseudonym: String, password: Password): UserToken?
    fun logout(userId: UserId, token: AccessToken): AccessToken?
    fun refresh(userId: UserId, token: AccessToken): UserToken?
    fun tokenBy(userId: UserId): UserToken?
    fun generateToken(user: User): AccessToken?
    fun deleteToken(userId: UserId)
    fun deleteTokens(tokens: List<AccessToken>)
    fun refreshTokenLifetime(userID: UserId, refreshLifeTime: Boolean = false): AccessToken?
    fun save(token: AccessToken): AccessToken?
    fun tokens(): List<AccessToken>
}