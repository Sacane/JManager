package fr.sacane.jmanager.app.adapters

import fr.sacane.jmanager.app.*
import fr.sacane.jmanager.domain.models.Account
import fr.sacane.jmanager.domain.models.Category
import fr.sacane.jmanager.domain.models.Response
import fr.sacane.jmanager.domain.models.UserId
import fr.sacane.jmanager.domain.hexadoc.LeftAdapter
import fr.sacane.jmanager.domain.port.api.TransactionReaderAdapter
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Service

@LeftAdapter
@Service
class TransactionValidator @Autowired constructor(private var apiPort: TransactionReaderAdapter) {

    fun findAccount(accountOwnerDTO: UserAccountDTO, tokenDTO: TokenDTO): ResponseEntity<AccountDTO> {
        val accountResponse =
            apiPort.findAccount(
                UserId(accountOwnerDTO.userCredentials.id),
                tokenDTO.toToken(),
                accountOwnerDTO.labelAccount
            )
        if(accountResponse.status.isFailure()) return ResponseEntity.badRequest().build()
        val account: AccountDTO = accountResponse.get()?.toDTO() ?: return ResponseEntity.badRequest().build()
        return ResponseEntity.ok(account)
    }
    fun getSheetAccountByDate(dto: UserSheetDTO, tokenDTO: TokenDTO): ResponseEntity<List<SheetDTO>>{
        val accountResponse = apiPort.findAccount(dto.userCredentials.id.id(), tokenDTO.toToken(), dto.accountLabel)
        if(accountResponse.status.isFailure()) return ResponseEntity.badRequest().build()
        val account = accountResponse.get() ?: return ResponseEntity.badRequest().build()
        return ResponseEntity.ok(account.sheets()?.filter{it.date.year == dto.year && it.date.month == dto.month}?.map { sheet -> sheet.toDTO() })
    }
    fun saveSheet(userId: Long, accountLabel: String, sheetDTO: SheetDTO, tokenDTO: TokenDTO): ResponseEntity<SheetSendDTO>{
        val queryResponse = apiPort.saveSheet(userId.id(), tokenDTO.toToken(), accountLabel, sheetDTO.toModel())
        if(queryResponse.status.isFailure()) return ResponseEntity.badRequest().build()
        return queryResponse.mapTo { sheetDTO.sheetToSend() }.toResponseEntity()
    }
    fun saveAccount(userAccount: UserAccountDTO, tokenDTO: TokenDTO) : ResponseEntity<AccountDTO>{
        val response = apiPort.saveAccount(userAccount.userCredentials.id.id(), tokenDTO.toToken(), Account(null, userAccount.amount, userAccount.labelAccount, mutableListOf()))
        if(response.get() == null) return ResponseEntity.badRequest().build()
        return response.mapTo { response.get()!!.toDTO() }.toResponseEntity()
    }
    fun getUserAccount(id: Long, tokenDTO: TokenDTO): ResponseEntity<List<AccountInfoDTO>> {
        val response = apiPort.getAccountByUser(id.id(), tokenDTO.toToken())
        if(response.get() == null) return ResponseEntity.badRequest().build()
        val mapped = response.mapTo { p -> p!!.map { AccountInfoDTO(it.amount(), it.label()) } }
        return mapped.toResponseEntity()
    }

    fun saveCategory(userCategoryDTO: UserCategoryDTO, token: TokenDTO): ResponseEntity<String>{
        val response = apiPort.addCategory(UserId(userCategoryDTO.userId), token.toToken(), Category(userCategoryDTO.label))
        if(response.status.isFailure()) return ResponseEntity.badRequest().build()
        return ResponseEntity.ok(response.get()!!.label)
    }

    fun retrieveAllCategories(userId: UserId, tokenDTO: TokenDTO): ResponseEntity<List<String>> {
        val response = apiPort.retrieveAllCategoryOfUser(userId, tokenDTO.toToken())
        if(response.status.isFailure()) return ResponseEntity.badRequest().build()
        return Response.ok(response.get()!!.map { u -> u.label }).toResponseEntity()
    }
    fun removeCategory(userCategoryDTO: UserCategoryDTO, tokenDTO: TokenDTO): ResponseEntity<String>{
        val response =  apiPort.removeCategory(userCategoryDTO.userId.id(), tokenDTO.toToken(), userCategoryDTO.label)
        if(response.status.isFailure()) return ResponseEntity.badRequest().build()
        return ResponseEntity.ok(response.get()?.label)
    }
    private fun SheetDTO.sheetToSend(): SheetSendDTO {
        return SheetSendDTO(this.label, this.amount, if(this.action) "Entree" else "Sortie", this.date)
    }

}