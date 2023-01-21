package fr.sacane.jmanager.infra.api

import fr.sacane.jmanager.domain.model.*
import fr.sacane.jmanager.domain.port.apiside.TransactionReader
import fr.sacane.jmanager.domain.port.serverside.UserTransaction
import org.springframework.beans.factory.annotation.Autowired

class TransactionReaderAdapter @Autowired constructor(private var apiPort: TransactionReader, private var userPort: UserTransaction) {

    fun findAccount(accountOwnerDTO: UserAccountDTO): AccountDTO?{
        val user = userPort.findById(accountOwnerDTO.userId.id())
        val account = user.accounts().find { account -> account.label() == accountOwnerDTO.labelAccount }
        return account?.toDTO()
    }
    fun getSheetAccountByDate(dto: UserSheetDTO): List<SheetDTO>?{
        val account = apiPort.findAccount(dto.userId.id(), dto.accountLabel)
        return if(account == null){
            null
        } else {
            account.sheets()?.filter{it.date.year == dto.year && it.date.month == dto.month}?.map { sheet -> sheet.toDTO() }
        }
    }
    fun saveSheet(userId: Long, accountLabel: String, sheetDTO: SheetDTO): Boolean{
        return apiPort.saveSheet(userId.id(), accountLabel, sheetDTO.toModel())
    }
    fun saveAccount(userAccount: UserAccountDTO) {
        apiPort.saveAccount(UserId(userAccount.userId), Account(null, userAccount.amount, userAccount.labelAccount, mutableListOf()))
    }
    fun getUserAccount(id: Long): List<AccountInfoDTO>? {
        return apiPort.getAccountByUser(id.id())?.map { AccountInfoDTO(it.amount(), it.label()) }
    }

    fun saveCategory(userCategoryDTO: UserCategoryDTO): Boolean{
        return apiPort.addCategory(UserId(userCategoryDTO.userId), Category(userCategoryDTO.label))
    }

    fun retrieveAllCategories(userId: Long): List<Category> {
        return apiPort.retrieveAllCategoryOfUser(userId)
    }
    fun removeCategory(userCategoryDTO: UserCategoryDTO): Boolean{
        return apiPort.removeCategory(userCategoryDTO.userId.id(), userCategoryDTO.label)
    }
}