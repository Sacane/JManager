package fr.sacane.jmanager.domain.port.apiside

import fr.sacane.jmanager.domain.model.User
import fr.sacane.jmanager.domain.model.UserId

interface UserRegisterFlow {
    fun registerUser(user: User): User
    fun findUserById(userId: UserId): User
}