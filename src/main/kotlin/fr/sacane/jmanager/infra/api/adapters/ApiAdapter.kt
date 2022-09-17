package fr.sacane.jmanager.infra.api.adapters

import com.toxicbakery.bcrypt.Bcrypt
import fr.sacane.jmanager.common.Hash
import fr.sacane.jmanager.domain.model.*
import fr.sacane.jmanager.domain.port.apiside.ApiPort
import org.springframework.beans.factory.annotation.Autowired

class ApiAdapter @Autowired constructor(private var apiPort: ApiPort) {

    /*
    * Mapping of domain -> dto
    */
    private fun User.toDTO(): UserDTO{
        return UserDTO(this.id.get(), this.username, "", this.pseudonym, this.email)
    }

    private fun Long.id(): UserId{
        return UserId(this)
    }

    private fun Sheet.toDTO(): SheetDTO{
        return SheetDTO(this.id, this.label, this.value, if(this.isEntry) "Recette" else "Debit", this.date)
    }

    private fun Account.toDTO(): AccountDTO{
        return AccountDTO(
            this.id(),
            this.amount(),
            this.label(),
            this.sheets()!!.map { sheet -> sheet.toDTO() }
        )
    }

    /**
     * Mapping of dto -> model
     */

    private fun SheetDTO.toModel(): Sheet{
        return Sheet(this.id, this.label, this.date, this.amount, this.action == "Recette")
    }

    private fun AccountDTO.toModel(): Account{
        return Account(this.id, this.amount, this.labelAccount, this.sheets.map { it.toModel() }.toMutableList())
    }


    private fun UserDTO.toModel(): User{
        return User(this.id.id(), this.username, this.email, this.pseudonym, mutableListOf(), Password(this.password))
    }


    suspend fun createUser(userDTO: UserDTO): UserDTO?{
        val user = apiPort.createUser(userDTO.toModel())
        return user?.toDTO()
    }


    suspend fun verifyUser(userDTO: UserPasswordDTO): UserDTO?{
        val user = apiPort.findUserByPseudonym(userDTO.username)
        println(user?.username)

        return if(user != null && apiPort.checkUser(userDTO.username, userDTO.password)){
            user.toDTO()
        } else {
            null
        }
    }

    suspend fun findAccount(accountOwnerDTO: UserAccountDTO): AccountDTO?{
        val user = apiPort.findUserById(accountOwnerDTO.userId.id())
        val account = user.accounts().find { account -> account.label() == accountOwnerDTO.labelAccount }
        return account?.toDTO()
    }

    suspend fun getSheetAccountByDate(dto: UserSheetDTO): List<SheetDTO>?{
        val account = apiPort.findAccount(dto.userId.id(), dto.accountLabel)
        return if(account == null){
            null
        } else {
            account.sheets()?.map { sheet -> sheet.toDTO() }
        }
    }

    suspend fun saveSheet(userId: Long, accountLabel: String, sheetDTO: SheetDTO): Boolean{
        return apiPort.saveSheet(userId.id(), accountLabel, sheetDTO.toModel())
    }

    suspend fun saveAccount(userAccount: UserAccountDTO) {
        apiPort.saveAccount(UserId(userAccount.userId), Account(null, userAccount.amount, userAccount.labelAccount, mutableListOf()))
    }

    suspend fun getUserAccount(id: Long): List<AccountInfoDTO>? {
        return apiPort.getAccountByUser(id.id())?.map { AccountInfoDTO(it.amount(), it.label()) }
    }

}