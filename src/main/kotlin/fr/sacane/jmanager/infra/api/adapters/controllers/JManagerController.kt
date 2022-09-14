package fr.sacane.jmanager.infra.api.adapters.controllers

import fr.sacane.jmanager.infra.api.adapters.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.net.http.HttpResponse

@RestController
class JManagerController {

    @Autowired
    private lateinit var apiAdapter: ApiAdapter

    @PostMapping(path= ["/user/verify"])
    suspend fun verifyUser(@RequestBody userDTO: UserPasswordDTO): ResponseEntity<UserDTO>{
        val user = apiAdapter.verifyUser(userDTO)
        return if(user != null){
            ResponseEntity.ok(user)
        } else {
            ResponseEntity(HttpStatus.UNAUTHORIZED)
        }
    }

    @PostMapping(path = ["/user/account"])
    suspend fun findAccount(@RequestBody accountOwnerDTO: UserAccountDTO): ResponseEntity<AccountDTO>{
        val account = apiAdapter.findAccount(accountOwnerDTO)
        return if(account == null) ResponseEntity(HttpStatus.NOT_FOUND) else ResponseEntity(account, HttpStatus.OK)
    }

    @PostMapping(path= ["/user/create"])
    suspend fun createUser(@RequestBody userDTO: UserDTO): ResponseEntity<UserDTO> {
        val created = apiAdapter.createUser(userDTO)
        return if(created != null) ResponseEntity(created, HttpStatus.OK) else ResponseEntity(HttpStatus.UNAUTHORIZED)
    }

    @PostMapping("/account/create")
    suspend fun createAccount(@RequestBody userAccount: UserAccountDTO){
        apiAdapter.saveAccount(userAccount)
    }

    @PostMapping("/sheet/create")
    suspend fun createSheet(@RequestBody userAccountSheetDTO: UserAccountSheetDTO): ResponseEntity<SheetDTO>{
        return if(apiAdapter.saveSheet(userAccountSheetDTO.userId, userAccountSheetDTO.accountLabel, userAccountSheetDTO.sheetDTO)){
            ResponseEntity(userAccountSheetDTO.sheetDTO, HttpStatus.OK)
        } else {
            ResponseEntity(HttpStatus.INTERNAL_SERVER_ERROR)
        }
    }

    @GetMapping(path = ["user/account/get/{id}"])
    suspend fun getAccounts(@PathVariable id: String): ResponseEntity<List<AccountInfoDTO>>{
        val accounts = apiAdapter.getUserAccount(id.toLong())
        return if(accounts == null) ResponseEntity(accounts, HttpStatus.OK) else ResponseEntity(HttpStatus.NOT_FOUND)
    }

}