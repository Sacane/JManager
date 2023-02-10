package fr.sacane.jmanager.domain.port.api

import fr.sacane.jmanager.domain.models.*
import java.time.Month

interface BudgetResolver {
    fun openAccount(userid: UserId, account: Account): Response<Account>
    fun createSheet(userId: UserId, sheet: Sheet): Response<Sheet>
    fun retrieveSheetsByMonth(userId: UserId, month: Month): Response<List<Sheet>>
    fun retrieveAllRegisteredAccounts(userId: UserId): Response<List<Account>>
    fun createCategory(userId: UserId, category: Category): Response<Category>
    fun retrieveCategories(userId: UserId): Response<List<Category>>
}