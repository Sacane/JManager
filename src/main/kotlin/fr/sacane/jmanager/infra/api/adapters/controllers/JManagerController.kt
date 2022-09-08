package fr.sacane.jmanager.infra.api.adapters.controllers

import fr.sacane.jmanager.domain.model.UserId
import fr.sacane.jmanager.domain.port.apiside.ApiPort
import fr.sacane.jmanager.infra.api.adapters.ApiAdapter
import fr.sacane.jmanager.infra.api.adapters.UserDTO
import fr.sacane.jmanager.infra.api.adapters.UserPasswordDTO
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.security.SecurityProperties.User
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RestController

@RestController
class JManagerController {

    @Autowired
    private lateinit var domainAdapter: ApiAdapter

    @PostMapping(path= ["/user/verify"])
    suspend fun verifyUser(userDTO: UserPasswordDTO): ResponseEntity<UserDTO>{
        val user = domainAdapter.verifyUser(userDTO)
        return if(user != null){
            ResponseEntity.ok(user)
        } else {
            ResponseEntity(HttpStatus.UNAUTHORIZED)
        }
    }



}