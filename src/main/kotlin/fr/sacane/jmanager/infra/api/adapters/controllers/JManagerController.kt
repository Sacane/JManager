package fr.sacane.jmanager.infra.api.adapters.controllers

import fr.sacane.jmanager.infra.api.adapters.*
import org.springframework.beans.factory.annotation.Autowired
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

    @PostMapping(path = ["/user/account"])
    suspend fun findAccount(accountOwnerDTO: UserAccount): ResponseEntity<AccountDTO>{
        val account = domainAdapter.findAccount(accountOwnerDTO)
        return if(account == null) ResponseEntity(HttpStatus.NOT_FOUND) else ResponseEntity(account, HttpStatus.OK)
    }

}