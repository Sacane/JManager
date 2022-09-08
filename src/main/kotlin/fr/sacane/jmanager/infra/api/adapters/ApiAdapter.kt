package fr.sacane.jmanager.infra.api.adapters

import fr.sacane.jmanager.domain.model.Account
import fr.sacane.jmanager.domain.model.User
import fr.sacane.jmanager.domain.model.UserId
import fr.sacane.jmanager.domain.port.apiside.ApiPort
import org.springframework.beans.factory.annotation.Autowired

class ApiAdapter @Autowired constructor(private var apiPort: ApiPort) {

    /*
    * Mapping of domain -> dto
    */
    private fun User.toDTO(): UserDTO{
        return UserDTO(this.id.get(), this.username, "", this.pseudonym, this.email)
    }

    /**
     * Mapping of dto -> model
     */




    suspend fun verifyUser(userDTO: UserPasswordDTO): UserDTO?{
        val user = apiPort.findUserByPseudonym(userDTO.username)
        return if(user.doesPwdMatch(userDTO.password)){
            user.toDTO()
        } else {
            null
        }
    }


}