package fr.sacane.jmanager.domain.port.spi

import fr.sacane.jmanager.domain.hexadoc.DomainSide
import fr.sacane.jmanager.domain.hexadoc.Port
import fr.sacane.jmanager.domain.models.*


@Port(DomainSide.DATASOURCE)
interface UserTransaction {
    fun findById(userId: UserId): Ticket?
    fun findUserById(userId: UserId): User?
    fun checkUser(pseudonym: String, pwd: Password): Ticket?
    fun findByPseudonym(pseudonym: String): User?
    fun create(user: User): User?
    fun register(user: User): User?
    fun getUserToken(userId: UserId): Token?
    fun upsert(user: User): User?
}