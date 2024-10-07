package fr.sacane.jmanager.infrastructure.rest.account

import fr.sacane.jmanager.domain.asTokenUUID
import fr.sacane.jmanager.domain.hexadoc.Adapter
import fr.sacane.jmanager.domain.hexadoc.Side
import fr.sacane.jmanager.domain.models.Account
import fr.sacane.jmanager.domain.models.Amount
import fr.sacane.jmanager.domain.models.ResponseState
import fr.sacane.jmanager.domain.port.api.AccountFeature
import fr.sacane.jmanager.infrastructure.rest.*
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.util.logging.Logger


@RestController
@RequestMapping("api/account")
@Adapter(Side.APPLICATION)
class AccountController (
    private val feature: AccountFeature
) {
    companion object {
        private val LOGGER: Logger = Logger.getLogger("AccountController")
    }

    @GetMapping(path = ["{id}/{label}"])
    fun findAccount(
        @PathVariable id: Long,
        @PathVariable label: String,
        @RequestHeader("Authorization") token: String
    )
            : ResponseEntity<AccountDTO> {
        val accounts = feature.findAllRegisteredAccounts(
            id.id(),
            token.asTokenUUID()
        )
        if (accounts.status == ResponseState.NOT_FOUND) return ResponseEntity.notFound().build()
        return accounts.map { list ->
            list.find { it.label == label }?.toDTO()!!
        }.toResponseEntity()
    }

    @PostMapping("create")
    fun createAccount(
        @RequestBody userAccount: UserAccountDTO,
        @RequestHeader("Authorization") token: String
    ): ResponseEntity<AccountInfoDTO> = feature.save(
    userAccount.id.id(),
    token.asTokenUUID(),
    Account(amount = Amount.fromString(userAccount.amount), labelAccount = userAccount.labelAccount))
    .map { AccountInfoDTO(it.amount.toString(), it.label) }
    .toResponseEntity()


    @GetMapping(path = ["{id}"])
    fun getAccounts(
        @PathVariable id: Long,
        @RequestHeader("Authorization") token: String
    ): ResponseEntity<List<AccountDTO>> {
        LOGGER.info("Trying to get accounts")
        val response = feature.findAllRegisteredAccounts(
            id.id(),
            token.asTokenUUID()
        )
        return response.map { accounts ->
            accounts.map {
                AccountDTO(
                    it.id,
                    it.amount.toString(),
                    it.label,
                    it.sheets().map { sheet -> sheet.toDTO() }
                )
            }
        }.toResponseEntity()
    }

    @PostMapping(path = ["update/{userID}"])
    fun updateAccount(
        @PathVariable userID: Long,
        @RequestBody account: AccountDTO,
        @RequestHeader("Authorization") token: String
    ): ResponseEntity<AccountDTO> =
        feature.editAccount(userID, account.toModel(), token.asTokenUUID())
            .map { it.toDTO() }.toResponseEntity()


    @DeleteMapping(path = ["{userId}/delete/{accountId}"])
    fun deleteAccount(
        @PathVariable userId: Long,
        @PathVariable accountId: Long,
        @RequestHeader("Authorization") token: String
    ): ResponseEntity<Nothing> = feature.deleteAccountById(userId.id(), accountId, token.asTokenUUID()).toResponseEntity()

    @GetMapping("/user/{userID}/find/{accountID}")
    fun findAccountById(
        @PathVariable("userID") userID: Long,
        @PathVariable("accountID") accountID: Long,
        @RequestHeader("Authorization") token: String
    ): ResponseEntity<AccountDTO> =
        feature.findAccountById(userID.id(), accountID, token.asTokenUUID())
            .map { it.toDTO() }.toResponseEntity()
}