package fr.sacane.jmanager.domain.port.serverside

import fr.sacane.jmanager.domain.model.*

interface LoginTransactor {
    fun login(userId: UserId, password: Password, token: Token): Ticket
    fun logout(userId: UserId, token: Token): Ticket
    fun refresh(userId: UserId, token: Token): Ticket
}