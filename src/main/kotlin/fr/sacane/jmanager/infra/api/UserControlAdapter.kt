package fr.sacane.jmanager.infra.api

import fr.sacane.jmanager.domain.port.apiside.UserRegister
import org.springframework.beans.factory.annotation.Autowired

class UserControlAdapter @Autowired constructor(private var port: UserRegister) {
    fun createUser(userDTO: RegisteredUserDTO): UserDTO?{
        TODO()
    }
    fun loginUser(userDTO: UserPasswordDTO): UserDTO?{
        TODO()
    }

}