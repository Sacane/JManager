package fr.sacane.jmanager.domain.port.spi

import fr.sacane.jmanager.domain.models.AccessToken
import fr.sacane.jmanager.domain.models.Role
import fr.sacane.jmanager.domain.models.UserId

interface TokenGenerator {
    fun generateToken(userId: UserId, username: String, roles: Set<Role> = setOf(Role.USER)): AccessToken
    fun readToken(token: String): AccessToken?
}