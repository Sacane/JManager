package fr.sacane.jmanager.domain.port.spi

import fr.sacane.jmanager.domain.hexadoc.Port
import fr.sacane.jmanager.domain.hexadoc.Side
import fr.sacane.jmanager.domain.models.Password
import fr.sacane.jmanager.domain.models.User
import fr.sacane.jmanager.domain.models.UserId


@Port(Side.DATASOURCE)
interface UserRepository {
    fun findUserById(userId: UserId): User?
    fun findUserByIdWithAccounts(userId: UserId): User?
    //fun findUserByIdWithSheets(userId: UserId): User?
    fun findByPseudonym(pseudonym: String): User?
    fun create(user: User): User?
    fun register(username: String, email: String, password: Password): User?
    fun upsert(user: User): User?
}