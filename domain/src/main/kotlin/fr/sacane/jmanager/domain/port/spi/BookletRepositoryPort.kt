package fr.sacane.jmanager.domain.port.spi

import fr.sacane.jmanager.domain.hexadoc.Port
import fr.sacane.jmanager.domain.hexadoc.Side
import fr.sacane.jmanager.domain.models.Booklet
import fr.sacane.jmanager.domain.models.UserId

@Port(Side.INFRASTRUCTURE)
interface BookletRepositoryPort {
    fun editFromAnother(booklet: Booklet): Booklet?
    fun save(ownerId: UserId, booklet: Booklet): Booklet?
    fun findAccountByIdWithTransactions(accountId: Long): Booklet?
    fun findAccountByLabelWithTransactions(userId: UserId, accountLabel: String): Booklet?
    fun deleteAccountById(accountId: Long)
    fun upsert(booklet: Booklet): Booklet
    fun update(booklet: Booklet)
    fun findBookletsForUser(userId: UserId): List<Booklet>
}