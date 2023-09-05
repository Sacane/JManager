package fr.sacane.jmanager.infrastructure.api.controller

import fr.sacane.jmanager.infrastructure.api.*
import fr.sacane.jmanager.infrastructure.api.adapters.UserControlAdapter
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.util.logging.Logger

@RestController
@RequestMapping("/user")
class ProfileController

(private val userAdapter: UserControlAdapter){

        private val LOGGER: Logger = Logger.getLogger("ProfileController")
    private fun extractToken(authorization: String): String{
        return authorization.replace("Bearer ", "");
    }

    @PostMapping(path= ["/auth"])
    fun login(@RequestBody userDTO: UserPasswordDTO): ResponseEntity<UserStorageDTO> {
        LOGGER.info("Trying to login and get access token")
        return userAdapter.loginUser(userDTO)
    }

    @PostMapping(path = ["/logout/{id}"])
    suspend fun logout(@PathVariable id: Long, @RequestHeader("Authorization") token: String): ResponseEntity<Nothing>{
        return userAdapter.logout(id, extractToken(token))
    }
    @PostMapping(path= ["/create"])
    suspend fun createUser(@RequestBody userDTO: RegisteredUserDTO): ResponseEntity<UserDTO> {
        LOGGER.info("received user from client : $userDTO")
        return userAdapter.createUser(userDTO)
    }
}