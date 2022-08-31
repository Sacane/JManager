package fr.sacane.jmanager.domain.port.apiside

import fr.sacane.jmanager.domain.model.User


interface UserApiPort {

    suspend fun registerUser(user: User): User

}
