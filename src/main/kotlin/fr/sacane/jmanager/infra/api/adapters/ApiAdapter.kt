package fr.sacane.jmanager.infra.api.adapters

import fr.sacane.jmanager.domain.model.UserId
import fr.sacane.jmanager.domain.port.apiside.ApiPort
import org.springframework.beans.factory.annotation.Autowired

class ApiAdapter @Autowired constructor(private var apiPort: ApiPort) {


    suspend fun verifyUser(userDTO: UserDTO): UserDTO?{
        val user = apiPort.findUserById(UserId(userDTO.id))
        return if(user.doesPwdMatch(userDTO.password)){
            userDTO
        } else {
            null
        }
    }


}