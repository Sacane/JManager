package fr.sacane.jmanager.domain.port.serverside

import fr.sacane.jmanager.domain.hexadoc.PortToRight
import fr.sacane.jmanager.domain.models.*

@PortToRight
interface UserTransaction {
    fun findById(userId: UserId): Ticket?
    fun checkUser(pseudonym: String, pwd: Password): Ticket?
    fun findByPseudonym(pseudonym: String): User?
    fun create(user: User): User?
    fun save(user: User): User?
    fun getUserToken(userId: UserId): Token?
}