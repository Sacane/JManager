package fr.sacane.jmanager.app


import fr.sacane.jmanager.JmanagerBackApplication
import fr.sacane.jmanager.app.adapters.TransactionValidator
import fr.sacane.jmanager.app.adapters.UserControlAdapter
import fr.sacane.jmanager.domain.models.UserId
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*


@RestController
class WebController {

    companion object{
        private val LOGGER: Logger = LoggerFactory.getLogger(JmanagerBackApplication::class.java.name)
    }


    @Autowired
    private lateinit var apiAdapter: TransactionValidator

    @Autowired
    private lateinit var userAdapter: UserControlAdapter


    @PostMapping(path= ["/user/auth"])
    suspend fun login(@RequestBody userDTO: UserPasswordDTO): ResponseEntity<UserTokenDTO>{
        return userAdapter.loginUser(userDTO)
    }

    @PostMapping(path = ["/user/logout/{id}"])
    suspend fun logout(@PathVariable id: Long, @RequestHeader token: TokenDTO): ResponseEntity<UserDTO>{
        return userAdapter.logout(id, token)
    }

    @PostMapping(path = ["/user/account"])
    suspend fun findAccount(@RequestBody accountOwnerDTO: UserAccountDTO, @RequestHeader token: TokenDTO): ResponseEntity<AccountDTO>{
        return apiAdapter.findAccount(accountOwnerDTO, token)
    }

    @PostMapping(path= ["/user/create"])
    suspend fun createUser(@RequestBody userDTO: RegisteredUserDTO): ResponseEntity<UserDTO> {
       return userAdapter.createUser(userDTO)
    }

    @PostMapping("/account/create")
    suspend fun createAccount(@RequestBody userAccount: UserAccountDTO, @RequestHeader tokenPair: TokenDTO){
        apiAdapter.saveAccount(userAccount, tokenPair)
    }

    @PostMapping("/sheet/save")
    suspend fun createSheet(@RequestBody userAccountSheetDTO: UserAccountSheetDTO, @RequestHeader token: TokenDTO): ResponseEntity<SheetSendDTO>{
        return apiAdapter.saveSheet(
            userAccountSheetDTO.userId,
            userAccountSheetDTO.accountLabel,
            userAccountSheetDTO.sheetDTO,
            token
        )
    }

    @GetMapping(path = ["user/accounts/get/{id}"])
    suspend fun getAccounts(@PathVariable id: Long, @RequestHeader token: TokenDTO): ResponseEntity<List<AccountInfoDTO>>{
        LOGGER.debug("Trying to get the user's accounts by id : $id")
        return apiAdapter.getUserAccount(id, token)
    }

    @PostMapping(path=["sheets/get"])
    suspend fun getSheets(@RequestBody dto: UserSheetDTO, @RequestHeader token: TokenDTO): ResponseEntity<List<SheetDTO>>{
        LOGGER.debug(dto.month.toString())
        return apiAdapter.getSheetAccountByDate(dto, token)
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
}

