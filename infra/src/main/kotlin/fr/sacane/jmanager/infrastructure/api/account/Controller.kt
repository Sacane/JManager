package fr.sacane.jmanager.infrastructure.api.account

import fr.sacane.jmanager.domain.asTokenUUID
import fr.sacane.jmanager.domain.hexadoc.Adapter
import fr.sacane.jmanager.domain.hexadoc.Side
import fr.sacane.jmanager.domain.models.Account
import fr.sacane.jmanager.domain.models.asCurrency
import fr.sacane.jmanager.domain.models.toAmount
import fr.sacane.jmanager.domain.port.api.AccountFeature
import fr.sacane.jmanager.infrastructure.api.id
import fr.sacane.jmanager.infrastructure.api.toDTO
import fr.sacane.jmanager.infrastructure.api.toModel
import fr.sacane.jmanager.infrastructure.api.toResponseEntity
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
    ): ResponseEntity<AccountDTO> {
        LOGGER.info("Search for the account $label of $id")
        return feature.findByLabelAndUserId(userId = id.id(), token = token.asTokenUUID(), label = label)
            .map {
                it.toDTO()
            }.toResponseEntity()
    }

    @PostMapping
    fun createAccount(
        @RequestBody userAccount: UserAccountRequest,
        @RequestHeader("Authorization") token: String
    ): ResponseEntity<AccountInfoDTO> {
        LOGGER.info("Booking a new Account...")
        return feature.save(
            userAccount.id.id(),
            token.asTokenUUID(),
            Account(amount = userAccount.amount.toAmount(userAccount.currency.asCurrency()), labelAccount = userAccount.labelAccount)
        ).map { AccountInfoDTO(it.amount.toStringValue(), it.label, it.id.toString(), it.amount.currency.symbol) }.toResponseEntity()
    }


    @GetMapping(path = ["{id}"])
    fun getAccounts(
        @PathVariable id: Long,
        @RequestHeader("Authorization") token: String
    ): ResponseEntity<List<AccountDTO>> {
        LOGGER.info("Request for accounts of user $id")
        val response = feature.findAllRegisteredAccounts(
            id.id(),
            token.asTokenUUID()
        )
        return response.map { accounts ->
            accounts.map {
                it.toDTO()
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


    @DeleteMapping(path = ["{userId}/{accountId}"])
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