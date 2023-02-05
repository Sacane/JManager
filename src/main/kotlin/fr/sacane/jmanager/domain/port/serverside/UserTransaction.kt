package fr.sacane.jmanager.domain.port.serverside

import fr.sacane.jmanager.common.hexadoc.PortToRight
import fr.sacane.jmanager.domain.model.*

@PortToRight
interface UserTransaction {
    fun findById(userId: UserId): Ticket?
    fun checkUser(pseudonym: String, pwd: Password): Ticket?
    fun findByPseudonym(pseudonym: String): Ticket?
    fun create(user: User): User?
    fun save(user: User): User?
    fun getUserToken(userId: UserId): Token?
}