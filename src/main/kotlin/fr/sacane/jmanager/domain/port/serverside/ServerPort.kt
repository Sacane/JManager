package fr.sacane.jmanager.domain.port.serverside

import fr.sacane.jmanager.domain.model.Account
import fr.sacane.jmanager.domain.model.Sheet
import fr.sacane.jmanager.domain.model.User
import fr.sacane.jmanager.domain.model.UserId
import java.time.Month


interface ServerPort {

    suspend fun getSheets(user: UserId, accountLabel: String): List<Sheet>
    suspend fun getSheetsByDateAndAccount(userId: UserId, month: Month, year: Int, labelAccount: String): List<Sheet>
    suspend fun getAccounts(user: UserId): List<Account>
    suspend fun saveUser(user: User): User
    suspend fun findUserById(userId: UserId): User
    suspend fun saveAccount(userId: UserId, account: Account)

}
