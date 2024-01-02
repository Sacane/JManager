package fr.sacane.jmanager.domain.port.spi

import fr.sacane.jmanager.domain.models.AccessToken

interface TokenManager {
    fun generateToken(): AccessToken
    fun checkAuthenticate(to: AccessToken, from: AccessToken): Boolean
}