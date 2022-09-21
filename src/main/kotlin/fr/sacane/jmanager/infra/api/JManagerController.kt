package fr.sacane.jmanager.infra.api

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*


@RestController
@CrossOrigin(origins = ["*"], allowedHeaders = ["*"]) //To change for production
class JManagerController {

    companion object{
        private val LOGGER: Logger = LoggerFactory.getLogger("JManagerController")
    }


    @Autowired
    private lateinit var apiAdapter: ApiAdapter

    @PostMapping(path= ["/user/auth"])
    suspend fun verifyUser(@RequestBody userDTO: UserPasswordDTO): ResponseEntity<UserDTO>{
        val user = apiAdapter.verifyUser(userDTO)
        println(user?.pseudonym)
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

    @GetMapping(path = ["user/accounts/get/{id}"])
    suspend fun getAccounts(@PathVariable id: Long): ResponseEntity<List<AccountInfoDTO>>{
        LOGGER.debug("Trying to get the user's accounts by id : $id")
        val accounts = apiAdapter.getUserAccount(id)
        return if(accounts != null) ResponseEntity(accounts, HttpStatus.OK) else ResponseEntity(HttpStatus.NOT_FOUND)
    }

    @PostMapping(path=["sheets/get"])
    suspend fun getSheets(@RequestBody dto: UserSheetDTO): List<SheetDTO>?{
        return apiAdapter.getSheetAccountByDate(dto)
    }
}