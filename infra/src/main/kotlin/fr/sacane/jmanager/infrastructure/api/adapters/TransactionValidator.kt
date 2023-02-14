package fr.sacane.jmanager.infrastructure.api.adapters

import fr.sacane.jmanager.domain.hexadoc.LeftAdapter
import fr.sacane.jmanager.domain.models.*
import fr.sacane.jmanager.domain.port.api.BudgetResolver
import fr.sacane.jmanager.infrastructure.api.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Service

@LeftAdapter
@Service
class TransactionValidator {

    @Autowired
    private lateinit var apiPort: BudgetResolver

    fun findAccount(id: Long, label: String, tokenDTO: TokenDTO): ResponseEntity<AccountDTO> {
        val accounts = apiPort.retrieveAllRegisteredAccounts(id.id(), tokenDTO.toToken())
        if(accounts.status == ResponseState.NOT_FOUND) return ResponseEntity.notFound().build()
        val account = accounts.get()?.find { it.label() == label }?.toDTO() ?: return ResponseEntity.badRequest().build()
        return ResponseEntity.ok(account)
    }
    fun getSheetAccountByDate(dto: UserSheetDTO, tokenDTO: TokenDTO): ResponseEntity<List<SheetDTO>>{
        val response = apiPort.retrieveSheetsByMonthAndYear(dto.userCredentials.id.id(), tokenDTO.toToken(), dto.month, dto.year, dto.accountLabel)
        if(response.status.isFailure()) return ResponseEntity.badRequest().build()
        return ResponseEntity.ok(response.get()?.map { it.toDTO()})
    }
    fun saveSheet(userId: Long, accountLabel: String, sheetDTO: SheetDTO, tokenDTO: TokenDTO): ResponseEntity<SheetSendDTO>{
        val queryResponse = apiPort.createSheetAndAssociateItWithAccount(userId.id(), tokenDTO.toToken(), accountLabel, sheetDTO.toModel())
        if(queryResponse.status.isFailure()) return ResponseEntity.badRequest().build()
        return queryResponse.mapTo { sheetDTO.sheetToSend() }.toResponseEntity()
    }
    fun saveAccount(userAccount: UserAccountDTO, tokenDTO: TokenDTO) : ResponseEntity<AccountDTO>{
        val response = apiPort.openAccount(userAccount.id.id(), tokenDTO.toToken(), Account(null, userAccount.amount, userAccount.labelAccount, mutableListOf()))
        if(response.get() == null) return ResponseEntity.badRequest().build()
        return response.mapTo { response.get()!!.toDTO() }.toResponseEntity()
    }
    fun getUserAccount(id: Long, tokenDTO: TokenDTO): ResponseEntity<List<AccountInfoDTO>> {
        val response = apiPort.retrieveAllRegisteredAccounts(id.id(), tokenDTO.toToken())
        if(response.get() == null) return ResponseEntity.badRequest().build()
        val mapped = response.mapTo { p -> p!!.map { AccountInfoDTO(it.amount(), it.label()) } }
        return mapped.toResponseEntity()
    }

    fun saveCategory(userCategoryDTO: UserCategoryDTO, token: TokenDTO): ResponseEntity<String>{
        val response = apiPort.createCategory(UserId(userCategoryDTO.userId), token.toToken(), Category(userCategoryDTO.label))
        if(response.status.isFailure()) return ResponseEntity.badRequest().build()
        return ResponseEntity.ok(response.get()!!.label)
    }

    fun retrieveAllCategories(userId: UserId, tokenDTO: TokenDTO): ResponseEntity<List<String>> {
        val response = apiPort.retrieveCategories(userId, tokenDTO.toToken())
        if(response.status.isFailure()) return ResponseEntity.badRequest().build()
        return Response.ok(response.get()!!.map { u -> u.label }).toResponseEntity()
    }
    fun removeCategory(userCategoryDTO: UserCategoryDTO, tokenDTO: TokenDTO): ResponseEntity<String>{
        return ResponseEntity.ok("TODO")
    }
    private fun SheetDTO.sheetToSend(): SheetSendDTO {
        return SheetSendDTO(this.label, this.amount, if(this.action) "Entree" else "Sortie", this.date)
    }

}