package fr.sacane.jmanager.domain.port.spi

import fr.sacane.jmanager.domain.hexadoc.RightPort
import fr.sacane.jmanager.domain.models.*

@RightPort
interface UserTransaction {
    fun findById(userId: UserId): Ticket?
    fun checkUser(pseudonym: String, pwd: Password): Ticket?
    fun findByPseudonym(pseudonym: String): User?
    fun create(user: User): User?
    fun register(user: User): User?
    fun getUserToken(userId: UserId): Token?
}