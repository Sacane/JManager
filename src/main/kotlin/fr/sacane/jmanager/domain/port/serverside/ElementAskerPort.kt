package fr.sacane.jmanager.domain.port.serverside

import fr.sacane.jmanager.domain.model.Account
import fr.sacane.jmanager.domain.model.Sheet
import fr.sacane.jmanager.domain.model.UserId
import java.time.Month


interface ElementAskerPort {

    suspend fun getSheets(user: UserId, accountLabel: String): List<Sheet>
    suspend fun getAccounts(user: UserId): List<Account>
    suspend fun getSheets(user: UserId, month: Month, year: Int, labelAccount: String)

}
