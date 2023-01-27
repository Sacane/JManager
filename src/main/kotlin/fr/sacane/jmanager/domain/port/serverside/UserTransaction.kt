package fr.sacane.jmanager.domain.port.serverside

import fr.sacane.jmanager.domain.model.AccessTicket
import fr.sacane.jmanager.domain.model.Password
import fr.sacane.jmanager.domain.model.User
import fr.sacane.jmanager.domain.model.UserId

interface UserTransaction {
    fun findById(userId: UserId): User
    fun checkUser(pseudonym: String, pwd: Password): AccessTicket
    fun findByPseudonym(pseudonym: String): User?
    fun create(user: User): User?
    fun save(user: User): User
}