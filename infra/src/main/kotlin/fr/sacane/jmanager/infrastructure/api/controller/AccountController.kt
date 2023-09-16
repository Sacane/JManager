package fr.sacane.jmanager.infrastructure.api.controller

import fr.sacane.jmanager.domain.models.UserId
import fr.sacane.jmanager.infrastructure.api.AccountDTO
import fr.sacane.jmanager.infrastructure.api.AccountInfoDTO
import fr.sacane.jmanager.infrastructure.api.UserAccountDTO
import fr.sacane.jmanager.infrastructure.api.adapters.TransactionValidator
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.util.logging.Logger


@RestController
@RequestMapping("/account")
class AccountController (
    private val transactionValidator: TransactionValidator
){

    private val LOGGER: Logger = Logger.getLogger("AccountController")
    @GetMapping(path = ["{id}/{label}"])
    fun findAccount(@PathVariable id: Long, @PathVariable label: String, @RequestHeader("Authorization") token: String): ResponseEntity<AccountDTO> {
        return transactionValidator.findAccount(id, label, token)
    }
    private fun extractToken(authorization: String): String{
        return authorization.replace("Bearer ", "");
    }
    @PostMapping("create")
    fun createAccount(@RequestBody userAccount: UserAccountDTO, @RequestHeader("Authorization") tokenPair: String): ResponseEntity<AccountInfoDTO>{
        LOGGER.info("Trying to create a new account")
        return transactionValidator.saveAccount(userAccount, extractToken(tokenPair))
    }
    @GetMapping(path = ["{id}"])
    fun getAccounts(@PathVariable id: Long, @RequestHeader("Authorization") token: String): ResponseEntity<List<AccountDTO>>
    = transactionValidator.getUserAccount(id, extractToken(token)).also {
        LOGGER.info("Trying to get the user's accounts by id : $id")
    }



    @PostMapping(path = ["update/{userID}"])
    fun updateAccount(@PathVariable userID: Long, @RequestBody account: AccountDTO, @RequestHeader("Authorization") token: String): ResponseEntity<AccountDTO>
    = transactionValidator.updateAccount(userID, account, extractToken(token))

    @DeleteMapping(path = ["{userId}/delete/{accountId}"])
    fun deleteAccount(
        @PathVariable userId: Long,
        @PathVariable accountId: Long,
        @RequestHeader("Authorization") token: String
    ): ResponseEntity<Nothing>
    = transactionValidator.deleteAccount(UserId(userId), accountId)


    @GetMapping("/user/{userID}/find/{accountID}")
    fun findAccountById(
        @PathVariable("userID") userID: Long,
        @PathVariable("accountID") accountID: Long,
        @RequestHeader("Authorization") token: String
    ): ResponseEntity<AccountDTO>
    = transactionValidator.findAccountById(userID, accountID, extractToken(token))

}