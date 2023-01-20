package fr.sacane.jmanager.infra.api

import fr.sacane.jmanager.domain.model.Password
import fr.sacane.jmanager.domain.port.serverside.UserTransaction
import org.springframework.beans.factory.annotation.Autowired

class UserControlAdapter @Autowired constructor(private var port: UserTransaction) {
    fun createUser(userDTO: RegisteredUserDTO): UserDTO?{
        val user = port.create(userDTO.toModel())
        return user?.toDTO()
    }
    fun verifyUser(userDTO: UserPasswordDTO): UserDTO?{
        val user = port.findByPseudonym(userDTO.username)

        return if(user != null && port.checkUser(userDTO.username, Password(userDTO.password))){
            user.toDTO()
        } else {
            null
        }
    }
}