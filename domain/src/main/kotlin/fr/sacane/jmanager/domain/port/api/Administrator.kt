package fr.sacane.jmanager.domain.port.api

import fr.sacane.jmanager.domain.hexadoc.LeftPort
import fr.sacane.jmanager.domain.models.Response
import fr.sacane.jmanager.domain.models.Ticket
import fr.sacane.jmanager.domain.models.User
import fr.sacane.jmanager.domain.models.UserId

@LeftPort
interface Administrator {
    fun login(): Response<Ticket>
    fun logout(): Response<Nothing>
    fun signIn(user: User): Response<User>
    fun find(userId: UserId): Response<User>
}