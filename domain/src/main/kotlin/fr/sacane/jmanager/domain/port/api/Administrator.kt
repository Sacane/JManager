package fr.sacane.jmanager.domain.port.api

import fr.sacane.jmanager.domain.hexadoc.LeftPort
import fr.sacane.jmanager.domain.models.*

@LeftPort
interface Administrator {
    fun login(pseudonym: String, userPassword: Password): Response<Ticket>
    fun logout(userId: UserId, userToken: Token): Response<Nothing>
    fun signIn(user: User): Response<User>
}