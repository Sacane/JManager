package fr.sacane.jmanager.infrastructure.api.adapters

import fr.sacane.jmanager.domain.hexadoc.Adapter
import fr.sacane.jmanager.domain.hexadoc.DomainSide
import fr.sacane.jmanager.domain.models.*
import fr.sacane.jmanager.domain.port.api.Administrator
import fr.sacane.jmanager.domain.port.api.TransactionResolver
import fr.sacane.jmanager.infrastructure.api.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Service
import java.util.*
import java.util.logging.Logger

@Adapter(DomainSide.API)
@Service
class TransactionValidator(private val userRepository: Administrator) {

    @Autowired
    private lateinit var apiPort: TransactionResolver

    companion object{
        private val LOGGER: Logger = Logger.getLogger(TransactionValidator::class.java.toString())
    }

    fun findAccount(id: Long, label: String, tokenDTO: String): ResponseEntity<AccountDTO> {
        val accounts = apiPort.retrieveAllRegisteredAccounts(id.id(), Token(UUID.fromString(tokenDTO), null, null))
        if(accounts.status == ResponseState.NOT_FOUND) return ResponseEntity.notFound().build()
        return accounts.map { list ->
            list?.find { it.label == label }?.toDTO()!!
        }.toResponseEntity()
    }
    fun getSheetAccountByDate(dto: UserSheetDTO, token: String): ResponseEntity<SheetsAndAverageDTO>{
        val response = apiPort.retrieveSheetsByMonthAndYear(dto.userId.id(), Token(UUID.fromString(token), null, UUID.randomUUID()), dto.month, dto.year, dto.accountLabel)
        if(response.status.isFailure()) return ResponseEntity.badRequest().build()
        return ResponseEntity.ok(SheetsAndAverageDTO(response.mapTo { it -> it!!.map { it.toDTO() } }, 0.0))
    }
    fun saveSheet(userId: Long, accountLabel: String, sheetDTO: SheetDTO, tokenDTO: String): ResponseEntity<SheetSendDTO>{
        val queryResponse = apiPort.createSheetAndAssociateItWithAccount(userId.id(), Token(UUID.fromString(tokenDTO), null, UUID.randomUUID()), accountLabel, sheetDTO.toModel())
        if(queryResponse.status.isFailure()) return ResponseEntity.badRequest().build()
        return queryResponse.map { SheetSendDTO(it!!.label, it.date, it.expenses, it.income, it.sold) }.toResponseEntity()
    }
    fun saveAccount(userAccount: UserAccountDTO, token: String) : ResponseEntity<AccountInfoDTO>{
        val response = apiPort.openAccount(userAccount.id.id(), Token(UUID.fromString(token), null, UUID.randomUUID()), Account(null, userAccount.amount, userAccount.labelAccount, mutableListOf()))
        if(response.isFailure()){
            return response.mapTo { ResponseEntity.badRequest().build() }
        }
        return response.map { AccountInfoDTO(it!!.sold, it.label) }.toResponseEntity()
    }
    fun getUserAccount(id: Long, token: String): ResponseEntity<List<AccountDTO>> {
        val response = apiPort.retrieveAllRegisteredAccounts(id.id(), Token(UUID.fromString(token), null, UUID.randomUUID()))
        if(response.isFailure()){
            return response.mapTo { ResponseEntity.badRequest().build() }
        }
        val mapped = response.map { p -> p!!.map { AccountDTO(it.id!!, it.sold, it.label,
            it.sheets().map { s -> s.toDTO() }) } }
        return mapped.toResponseEntity()
    }

    fun saveCategory(userCategoryDTO: UserCategoryDTO, token: TokenDTO): ResponseEntity<String>{
        val response = apiPort.createCategory(UserId(userCategoryDTO.userId), token.toToken(), Category(userCategoryDTO.label))
        if(response.status.isFailure()) return ResponseEntity.badRequest().build()
        return response.map{ it?.label ?: "OK" }.toResponseEntity()
    }

    fun retrieveAllCategories(userId: UserId, tokenDTO: TokenDTO): ResponseEntity<List<String>> {
        val response = apiPort.retrieveCategories(userId, tokenDTO.toToken())
        if(response.status.isFailure()) return ResponseEntity.badRequest().build()
        return response.map { u -> u!!.map { it.label } }.toResponseEntity()
    }
    fun removeCategory(userCategoryDTO: UserCategoryDTO, tokenDTO: TokenDTO): ResponseEntity<String>{
        return ResponseEntity.ok("TODO")
    }

    fun deleteSheetByIds(ids: AccountSheetIdsDTO): ResponseEntity<Nothing>{
        return apiPort.deleteSheetsByIds(ids.accountId, ids.sheetIds).let {
            ResponseEntity.ok().build()
        }
    }
    fun deleteAccount(userId: UserId, accountID: Long): ResponseEntity<Nothing>{
        return apiPort.deleteAccountById(userId, accountID).toResponseEntity()
    }

    fun updateAccount(userID: Long, account: AccountDTO, extractToken: String): ResponseEntity<AccountDTO> {
        return apiPort.editAccount(userID, account.toModel(), Token(UUID.fromString(extractToken)))
            .map { it!!.toDTO() }.toResponseEntity()
    }
    fun editSheet(userID: Long, accountID: Long, sheet: SheetDTO, token: String): ResponseEntity<SheetDTO>{
        return apiPort.editSheet(userID, accountID, sheet.toModel(), Token(UUID.fromString(token)))
            .mapBoth(
                {s -> ResponseEntity.ok(s!!.toDTO()) },
                {ResponseEntity.badRequest().build()}
            ) ?: ResponseEntity.badRequest().build()
    }

    fun findSheetById(userID: Long, id: Long, extractToken: String): ResponseEntity<SheetDTO> {
        return apiPort.findById(userID, id, Token(UUID.fromString(extractToken)))
            .mapTo {
                it ?: Response.invalid<SheetDTO>()
                Response.ok(it)
            }.map {
                it!!.toDTO()
            }.toResponseEntity()
    }

    fun findAccountById(userID: Long, accountID: Long, token: String): ResponseEntity<AccountDTO> {
        return apiPort.findAccountById(UserId(userID), accountID, Token(UUID.fromString(token)))
            .mapTo {
                Response.ok(it!!.toDTO())
            }.toResponseEntity()
    }
}