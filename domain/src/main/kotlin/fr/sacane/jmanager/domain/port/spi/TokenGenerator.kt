package fr.sacane.jmanager.domain.port.spi

import fr.sacane.jmanager.domain.models.AccessToken
import fr.sacane.jmanager.domain.models.Role
import fr.sacane.jmanager.domain.models.UserId

interface TokenGenerator {
    fun generateToken(userId: UserId, username: String, role: Role = Role.USER): AccessToken
    fun readToken(token: String): AccessToken?
}