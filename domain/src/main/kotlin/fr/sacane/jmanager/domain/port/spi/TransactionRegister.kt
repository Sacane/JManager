package fr.sacane.jmanager.domain.port.spi
import fr.sacane.jmanager.domain.hexadoc.RightPort
import fr.sacane.jmanager.domain.models.Account
import fr.sacane.jmanager.domain.models.Category
import fr.sacane.jmanager.domain.models.Sheet
import fr.sacane.jmanager.domain.models.UserId
import java.time.Month

@RightPort
interface TransactionRegister {

    fun persist(userId: UserId, account: Account): Account?
    fun persist(userId: UserId, accountLabel: String, sheet: Sheet): Sheet?
    fun persist(userId: UserId, category: Category): Category?
    fun removeCategory(userId: UserId, labelCategory: String): Category?
    fun remove(targetCategory: Category)
    fun persist(account: Account) :Account?
}
