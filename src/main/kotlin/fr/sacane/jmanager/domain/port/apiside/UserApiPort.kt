package fr.sacane.jmanager.domain.port.apiside

import fr.sacane.jmanager.domain.model.Account
import fr.sacane.jmanager.domain.model.User
import fr.sacane.jmanager.domain.model.UserId
import java.time.Month


interface UserApiPort {

    suspend fun registerUser(user: User): User
    suspend fun findUserById(userId: UserId): User
    suspend fun saveAccount(userId: String, account: Account)
    suspend fun sheetByDateAndAccount(userId: UserId, month: Month, year: Int, account: String)

}
