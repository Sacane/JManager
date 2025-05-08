package fr.sacane.jmanager.infrastructure.api.account

import fr.sacane.jmanager.domain.asTokenUUID
import fr.sacane.jmanager.domain.hexadoc.Adapter
import fr.sacane.jmanager.domain.hexadoc.Side
import fr.sacane.jmanager.domain.models.Account
import fr.sacane.jmanager.domain.models.asCurrency
import fr.sacane.jmanager.domain.models.toAmount
import fr.sacane.jmanager.domain.port.api.AccountFeature
import fr.sacane.jmanager.infrastructure.api.toDTO
import fr.sacane.jmanager.infrastructure.api.toHttpResponse
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
        @RequestBody userAccount: BookletBookingRequest,
        @RequestHeader("Authorization") token: String
    ): ResponseEntity<AccountInfoDTO> {
        LOGGER.info("Booking a new Account...")
        return feature.save(
            token.asTokenUUID(),
            Account(amount = userAccount.amount.toAmount(userAccount.currency.asCurrency()), labelAccount = userAccount.labelAccount)
        ).map { AccountInfoDTO(it.amount.toStringValue(), it.label, it.id.toString(), it.amount.currency.symbol) }.toHttpResponse()
    }


    @GetMapping
    fun getAccounts(
        @RequestHeader("Authorization") token: String
    ): ResponseEntity<List<AccountDTO>> {
        val response = feature.findAllRegisteredAccounts(
            token.asTokenUUID()
        )
        return response.map { accounts ->
            accounts.map {
                it.toDTO()
            }
        }.toHttpResponse()
    }

    @DeleteMapping(path = ["{accountId}"])
    fun deleteAccount(
        @PathVariable accountId: Long,
        @RequestHeader("Authorization") token: String
    ): ResponseEntity<Nothing> = feature.deleteAccountById(accountId, token.asTokenUUID()).toHttpResponse()

    @GetMapping("{accountID}")
    fun findAccountById(
        @PathVariable("accountID") accountID: Long,
        @RequestHeader("Authorization") token: String
    ): ResponseEntity<AccountDTO> =
        feature.findAccountById(accountID, token.asTokenUUID())
            .map { it.toDTO() }.toHttpResponse()
}