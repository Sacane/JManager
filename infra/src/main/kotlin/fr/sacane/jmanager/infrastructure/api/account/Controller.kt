package fr.sacane.jmanager.infrastructure.api.account

import fr.sacane.jmanager.domain.hexadoc.Adapter
import fr.sacane.jmanager.domain.hexadoc.Side
import fr.sacane.jmanager.domain.models.Account
import fr.sacane.jmanager.domain.models.asCurrency
import fr.sacane.jmanager.domain.models.toAmount
import fr.sacane.jmanager.domain.port.api.BookletFeature
import fr.sacane.jmanager.infrastructure.api.currentUser
import fr.sacane.jmanager.infrastructure.api.toDTO
import fr.sacane.jmanager.infrastructure.api.toHttpResponse
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.util.logging.Logger


@RestController
@RequestMapping("api/account")
@Adapter(Side.APPLICATION)
class AccountController (
    private val feature: BookletFeature
) {
    companion object {
        private val LOGGER: Logger = Logger.getLogger("AccountController")
    }

    @PostMapping
    fun createAccount(
        @RequestBody userAccount: BookletBookingRequest
    ): ResponseEntity<AccountInfoDTO> {
        LOGGER.info("Booking a new Account...")
        return feature.save(
            currentUser.token,
            Account(amount = userAccount.amount.toAmount(userAccount.currency.asCurrency()), labelAccount = userAccount.labelAccount)
        ).map { AccountInfoDTO(it.amount.toStringValue(), it.label, it.id.toString(), it.amount.currency.symbol) }.toHttpResponse()
    }

    @GetMapping
    fun getAccounts(): ResponseEntity<List<AccountDTO>> {
        LOGGER.info("Requesting all accounts...")

        val response = feature.findAllRegisteredAccounts(
            currentUser.token
        )
        return response.map { accounts ->
            accounts.map {
                it.toDTO()
            }
        }.toHttpResponse()
    }

    @DeleteMapping(path = ["{accountId}"])
    fun deleteAccount(
        @PathVariable accountId: Long
    ): ResponseEntity<Nothing> = feature.deleteAccountById(accountId, currentUser.token).toHttpResponse()

    @GetMapping("{accountID}")
    fun findAccountById(
        @PathVariable("accountID") accountID: Long
    ): ResponseEntity<AccountDTO> =
        feature.findAccountById(accountID, currentUser.token)
            .map { it.toDTO() }.toHttpResponse()
}