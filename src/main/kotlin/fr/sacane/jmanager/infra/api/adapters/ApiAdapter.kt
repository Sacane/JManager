package fr.sacane.jmanager.infra.api.adapters

import fr.sacane.jmanager.domain.model.Account
import fr.sacane.jmanager.domain.model.Sheet
import fr.sacane.jmanager.domain.model.User
import fr.sacane.jmanager.domain.model.UserId
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
        return SheetDTO(this.label, this.value, if(this.isEntry) "Recette" else "Debit", this.date)
    }

    private fun Account.toDTO(): AccountDTO{
        return AccountDTO(
            this.amount(),
            this.label(),
            this.sheets()!!.map { sheet -> sheet.toDTO() }
        )
    }
    /**
     * Mapping of dto -> model
     */




    suspend fun verifyUser(userDTO: UserPasswordDTO): UserDTO?{
        val user = apiPort.findUserByPseudonym(userDTO.username)
        return if(user.doesPwdMatch(userDTO.password)){
            user.toDTO()
        } else {
            null
        }
    }

    suspend fun findAccount(accountOwnerDTO: UserAccount): AccountDTO?{
        val user = apiPort.findUserById(accountOwnerDTO.userId.id())
        val account = user.accounts().find { account -> account.label() == accountOwnerDTO.labelAccount }
        return account?.toDTO()
    }

}