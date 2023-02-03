package fr.sacane.jmanager.domain.port.apiside

import fr.sacane.jmanager.domain.model.Password
import fr.sacane.jmanager.domain.model.Response
import fr.sacane.jmanager.domain.model.User
import fr.sacane.jmanager.domain.model.UserId

interface UserRegister {
    fun signOut(user: User): Response<User>
    fun login(userId: UserId, userPassword: Password, userToken: String, pwd:String): Response<*>
}