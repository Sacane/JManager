package fr.sacane.jmanager.domain.port.api

import fr.sacane.jmanager.domain.models.*
import java.time.Month

interface BudgetResolver {
    fun openAccount(userId: UserId, token: Token, account: Account): Response<Account>
    fun createSheetAndAssociateItWithAccount(userId: UserId, token: Token, accountLabel: String, sheet: Sheet): Response<Sheet>
    fun retrieveSheetsByMonthAndYear(userId: UserId, token: Token, month: Month, year: Int, account: String): Response<List<Sheet>>
    fun retrieveAllRegisteredAccounts(userId: UserId, token: Token): Response<List<Account>>
    fun createCategory(userId: UserId, token: Token, category: Category): Response<Category>
    fun retrieveCategories(userId: UserId, token: Token): Response<List<Category>>
}