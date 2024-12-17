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
import fr.sacane.jmanager.infrastructure.api.sendAsHttpResponse
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

    @PostMapping
    fun createAccount(
        @RequestBody userAccount: UserBookletRequest,
        @RequestHeader("Authorization") token: String
    ): ResponseEntity<AccountInfoDTO> {
        LOGGER.info("Booking a new Account...")
        return feature.save(
            userAccount.id.id(),
            token.asTokenUUID(),
            Account(amount = userAccount.amount.toAmount(userAccount.currency.asCurrency()), labelAccount = userAccount.labelAccount)
        ).map { AccountInfoDTO(it.amount.toStringValue(), it.label, it.id.toString(), it.amount.currency.symbol) }.sendAsHttpResponse()
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
        }.sendAsHttpResponse()
    }

    @DeleteMapping(path = ["{userId}/{accountId}"])
    fun deleteAccount(
        @PathVariable userId: Long,
        @PathVariable accountId: Long,
        @RequestHeader("Authorization") token: String
    ): ResponseEntity<Nothing> = feature.deleteAccountById(userId.id(), accountId, token.asTokenUUID()).sendAsHttpResponse()

    @GetMapping("{accountID}/user/{userID}")
    fun findAccountById(
        @PathVariable("userID") userID: Long,
        @PathVariable("accountID") accountID: Long,
        @RequestHeader("Authorization") token: String
    ): ResponseEntity<AccountDTO> =
        feature.findAccountById(userID.id(), accountID, token.asTokenUUID())
            .map { it.toDTO() }.sendAsHttpResponse()
}