package fr.sacane.jmanager.infra.api

import fr.sacane.jmanager.domain.port.apiside.UserRegisterFlow
import org.springframework.beans.factory.annotation.Autowired

class UserControlAdapter @Autowired constructor(private var port: UserRegisterFlow) {
    fun createUser(userDTO: RegisteredUserDTO): UserDTO?{
        val user = port.createUser(userDTO.toModel())
        return user?.toDTO()
    }
    fun verifyUser(userDTO: UserPasswordDTO): UserDTO?{
        val user = port.findUserByPseudonym(userDTO.username)

        return if(user != null && port.checkUser(userDTO.username, userDTO.password)){
            user.toDTO()
        } else {
            null
        }
    }
}