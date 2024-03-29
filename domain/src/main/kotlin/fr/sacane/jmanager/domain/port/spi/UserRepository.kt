package fr.sacane.jmanager.domain.port.spi

import fr.sacane.jmanager.domain.hexadoc.Side
import fr.sacane.jmanager.domain.hexadoc.Port
import fr.sacane.jmanager.domain.models.*


@Port(Side.DATASOURCE)
interface UserRepository {
    fun findUserById(userId: UserId): User?
    fun checkUser(pseudonym: String, pwd: Password): UserToken?
    fun findByPseudonym(pseudonym: String): User?
    fun create(user: User): User?
    fun register(user: User): User?
    fun getUserToken(userId: UserId): AccessToken?
    fun upsert(user: User): User?
}