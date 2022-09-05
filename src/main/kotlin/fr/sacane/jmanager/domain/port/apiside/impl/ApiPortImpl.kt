package fr.sacane.jmanager.domain.port.apiside.impl

import fr.sacane.jmanager.domain.model.Account
import fr.sacane.jmanager.domain.model.Sheet
import fr.sacane.jmanager.domain.model.User
import fr.sacane.jmanager.domain.model.UserId
import fr.sacane.jmanager.domain.port.apiside.ApiPort
import fr.sacane.jmanager.domain.port.serverside.ServerPort
import java.time.Month

class ApiPortImpl(private val port: ServerPort): ApiPort {

    override suspend fun registerUser(user: User): User {
        return port.saveUser(user)
    }

    override suspend fun findUserById(userId: UserId): User {
        return port.findUserById(userId)
    }

    override suspend fun saveAccount(userId: UserId, account: Account) {
        return port.saveAccount(userId, account)
    }

    override suspend fun sheetByDateAndAccount(userId: UserId, month: Month, year: Int, account: String): List<Sheet> {
        return port.getSheetsByDateAndAccount(userId, month, year, account)
    }
}