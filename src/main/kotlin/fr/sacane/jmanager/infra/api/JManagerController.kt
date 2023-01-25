package fr.sacane.jmanager.infra.api

import fr.sacane.jmanager.JmanagerBackApplication
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*


@RestController
class JManagerController {

    companion object{
        private val LOGGER: Logger = LoggerFactory.getLogger(JmanagerBackApplication::class.java.name)
    }


    @Autowired
    private lateinit var apiAdapter: TransactionValidator

    @Autowired
    private lateinit var userAdapter: UserControlAdapter

    @PostMapping(path= ["/user/auth"])
    suspend fun verifyUser(@RequestBody userDTO: UserPasswordDTO): ResponseEntity<UserDTO>{
        val user = userAdapter.verifyUser(userDTO)
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
    suspend fun createUser(@RequestBody userDTO: RegisteredUserDTO): ResponseEntity<UserDTO> {
        val created = userAdapter.createUser(userDTO)
        return if(created != null) ResponseEntity(created, HttpStatus.OK) else ResponseEntity(HttpStatus.UNAUTHORIZED)
    }

    @PostMapping("/account/create")
    suspend fun createAccount(@RequestBody userAccount: UserAccountDTO){
        apiAdapter.saveAccount(userAccount)
    }

    @PostMapping("/sheet/save")
    suspend fun createSheet(@RequestBody userAccountSheetDTO: UserAccountSheetDTO): ResponseEntity<SheetSendDTO>{
        return if(apiAdapter.saveSheet(userAccountSheetDTO.userId, userAccountSheetDTO.accountLabel, userAccountSheetDTO.sheetDTO)){
            ResponseEntity(userAccountSheetDTO.sheetDTO.sheetToSend(), HttpStatus.OK)
        } else {
            ResponseEntity(HttpStatus.BAD_REQUEST)
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
        LOGGER.debug(dto.month.toString())
        return apiAdapter.getSheetAccountByDate(dto)
    }
    @PostMapping(path = ["user/category"])
    suspend fun saveUserCategory(@RequestBody userCategoryDTO: UserCategoryDTO): ResponseEntity<String>{
        LOGGER.info("Add a new Category")
        return if(apiAdapter.saveCategory(userCategoryDTO)){
            ResponseEntity.ok(userCategoryDTO.label)
        } else {
            ResponseEntity.badRequest().build()
        }
    }

    @GetMapping(path = ["user/categories/{userId}"])
    suspend fun retrieveAllUserCategories(@PathVariable userId: String): ResponseEntity<List<String>>{
        val categories = apiAdapter.retrieveAllCategories(userId.toLong())
        return if(categories.isEmpty()){
            ResponseEntity.notFound().build()
        } else {
            ResponseEntity.ok(categories.map { it.label })
        }
    }
    @DeleteMapping(path=["category/delete"])
    suspend fun deleteCategory(@RequestBody userCategoryDTO: UserCategoryDTO): ResponseEntity<Unit>{
        return if(apiAdapter.removeCategory(userCategoryDTO)) ResponseEntity.ok(null) else ResponseEntity.notFound().build()
    }
}

private fun SheetDTO.sheetToSend(): SheetSendDTO{
    return SheetSendDTO(this.label, this.amount, if(this.action) "Entree" else "Sortie", this.date)
}
