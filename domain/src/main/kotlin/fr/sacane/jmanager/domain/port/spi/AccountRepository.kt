package fr.sacane.jmanager.domain.port.spi

import fr.sacane.jmanager.domain.models.Account

interface AccountRepository {
    fun editFromAnother(account: Account): Account?
    fun getLastSheetPosition(accountId: Long): Int?
}