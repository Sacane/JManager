package fr.sacane.jmanager.infrastructure.api


import fr.sacane.jmanager.infrastructure.JmanagerBackApplication
import fr.sacane.jmanager.infrastructure.api.adapters.TransactionValidator
import fr.sacane.jmanager.infrastructure.api.adapters.UserControlAdapter
import fr.sacane.jmanager.domain.models.UserId
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*


@RestController
@RequestMapping(produces = [MediaType.APPLICATION_JSON_VALUE])
class WebController {

    companion object{
        private val LOGGER: Logger = LoggerFactory.getLogger(JmanagerBackApplication::class.java.name)
    }


    @Autowired
    private lateinit var apiAdapter: TransactionValidator

    @Autowired
    private lateinit var userAdapter: UserControlAdapter


    private fun extractToken(authorization: String): String{
        return authorization.replace("Bearer ", "");
    }

    @PostMapping(path= ["/user/auth"])
    suspend fun login(@RequestBody userDTO: UserPasswordDTO): ResponseEntity<UserStorageDTO>{
        LOGGER.info("Trying to login and get access token")
        return userAdapter.loginUser(userDTO)
    }

    @PostMapping(path = ["/user/logout/{id}"])
    suspend fun logout(@PathVariable id: Long, @RequestHeader("Authorization") token: String): ResponseEntity<Nothing>{
        return userAdapter.logout(id, extractToken(token))
    }

    @GetMapping(path = ["/user/{id}/account/{label}"])
    suspend fun findAccount(@PathVariable id: Long, @PathVariable label: String, @RequestHeader("Authorization") token: String): ResponseEntity<AccountDTO>{
        return apiAdapter.findAccount(id, label, token)
    }

    @PostMapping(path= ["/user/create"])
    suspend fun createUser(@RequestBody userDTO: RegisteredUserDTO): ResponseEntity<UserDTO> {
        LOGGER.info("received user from client : $userDTO")
       return userAdapter.createUser(userDTO)
    }

    @PostMapping("/account/create")
    suspend fun createAccount(@RequestBody userAccount: UserAccountDTO, @RequestHeader("Authorization") tokenPair: String): ResponseEntity<AccountInfoDTO>{
        LOGGER.info("Trying to create a new account")
        return apiAdapter.saveAccount(userAccount, extractToken(tokenPair))
    }

    @PostMapping("/sheet/save")
    suspend fun createSheet(@RequestBody userAccountSheetDTO: UserAccountSheetDTO, @RequestHeader("Authorization") token: String): ResponseEntity<SheetSendDTO>{
        return apiAdapter.saveSheet(
            userAccountSheetDTO.userId,
            userAccountSheetDTO.accountLabel,
            userAccountSheetDTO.sheetDTO,
            extractToken(token)
        ).apply { LOGGER.info("Sheet has been created") }
    }

    @DeleteMapping("/sheet/delete")
    fun deleteByIds(@RequestBody sheetIds: AccountSheetIdsDTO, @RequestHeader("Authorization") token: String): ResponseEntity<Nothing>{
        return apiAdapter.deleteSheetByIds(sheetIds)
    }

    @GetMapping(path = ["user/accounts/get/{id}"])
    suspend fun getAccounts(@PathVariable id: Long, @RequestHeader("Authorization") token: String): ResponseEntity<List<AccountDTO>>{
        LOGGER.debug("Trying to get the user's accounts by id : $id")
        return apiAdapter.getUserAccount(id, extractToken(token))
    }

    @PostMapping(path=["sheets/get"])
    suspend fun getSheets(@RequestBody dto: UserSheetDTO, @RequestHeader("Authorization") token: String): ResponseEntity<SheetsAndAverageDTO>{
        LOGGER.info("CHECK FOR SHEETS : $dto")
        return apiAdapter.getSheetAccountByDate(dto, extractToken(token))
    }
    @PostMapping(path = ["user/category"])
    suspend fun saveUserCategory(@RequestBody userCategoryDTO: UserCategoryDTO, @RequestHeader token: TokenDTO): ResponseEntity<String>{
        LOGGER.info("Add a new Category")
        return apiAdapter.saveCategory(userCategoryDTO, token)
    }

    @GetMapping(path = ["user/categories/{userId}"])
    suspend fun retrieveAllUserCategories(@PathVariable userId: String, @RequestHeader token: TokenDTO): ResponseEntity<List<String>>{
        return apiAdapter.retrieveAllCategories(UserId(userId.toLong()), token)
    }
    @DeleteMapping(path=["category/delete"])
    suspend fun deleteCategory(@RequestBody userCategoryDTO: UserCategoryDTO, @RequestHeader token: TokenDTO): ResponseEntity<String>{
        return apiAdapter.removeCategory(userCategoryDTO, token)
    }

    @DeleteMapping(path = ["user/{userId}/account/delete/{accountId}"])
    fun deleteAccount(@PathVariable userId: Long, @PathVariable accountId: Long, @RequestHeader token: TokenDTO): ResponseEntity<Nothing> {
        return apiAdapter.deleteAccount(UserId(userId), accountId)
    }
}
