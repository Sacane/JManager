package fr.sacane.jmanager.domain.port.apiside

import fr.sacane.jmanager.domain.model.Account
import fr.sacane.jmanager.domain.model.Sheet
import fr.sacane.jmanager.domain.model.User
import fr.sacane.jmanager.domain.model.UserId
import java.time.Month


interface ApiPort {

    suspend fun registerUser(user: User): User
    suspend fun findUserById(userId: UserId): User
    suspend fun findUserByPseudonym(pseudonym: String): User
    suspend fun saveAccount(userId: UserId, account: Account)
    suspend fun sheetByDateAndAccount(userId: UserId, month: Month, year: Int, account: String): List<Sheet>


}
