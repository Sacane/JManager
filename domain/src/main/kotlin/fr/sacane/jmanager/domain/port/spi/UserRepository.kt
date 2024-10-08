package fr.sacane.jmanager.domain.port.spi

import fr.sacane.jmanager.domain.hexadoc.Port
import fr.sacane.jmanager.domain.hexadoc.Side
import fr.sacane.jmanager.domain.models.Password
import fr.sacane.jmanager.domain.models.User
import fr.sacane.jmanager.domain.models.UserId
import fr.sacane.jmanager.domain.models.UserWithPassword


@Port(Side.INFRASTRUCTURE)
interface UserRepository {
    fun findUserById(userId: UserId): User?
    fun findUserByIdWithAccounts(userId: UserId): User?
    //fun findUserByIdWithSheets(userId: UserId): User?
    fun findByPseudonym(pseudonym: String): User?
    fun findByPseudonymWithEncodedPassword(pseudonym: String): UserWithPassword?
    fun create(user: UserWithPassword): User?
    fun register(username: String, email: String, password: Password): User?
    fun upsert(user: User): User?
}