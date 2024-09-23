package fr.sacane.jmanager.domain.port

import fr.sacane.jmanager.domain.models.*
import fr.sacane.jmanager.domain.port.spi.AccountRepository
import fr.sacane.jmanager.domain.port.spi.TransactionRepositoryPort
import fr.sacane.jmanager.domain.port.spi.UserRepository
import java.util.Random

class InMemoryTransactionRepository(
    private val inMemoryUserProvider: InMemoryUserProvider
): TransactionRepositoryPort {

    private val tags = mutableSetOf<Tag>()
    private val transactions = mutableSetOf<Transaction>()
    private val accounts = mutableSetOf<Account>()

    override fun persist(userId: UserId, account: Account): User? {
        val user = inMemoryUserProvider.users[userId]?.user
        user?.addAccount(account)
        return user
    }

    override fun persist(userId: UserId, accountLabel: String, transaction: Transaction): Transaction? {
        inMemoryUserProvider.users[userId]?.user
            ?.accounts
            ?.find { it.label == accountLabel }
        ?.transactions
        ?.add(transaction) ?: return null
        return transaction
    }

    override fun persist(userId: UserId, category: Tag): Tag? {
        tags.add(category)
        inMemoryUserProvider.users[userId]
            ?.user?.tags?.add(category) ?: return null
        return category
    }

    override fun persist(account: Account): Account? {
        inMemoryUserProvider.users[account.owner?.id]?.user?.addAccount(account) ?: return null
        return account
    }

    override fun findAccountByLabel(userId: UserId, labelAccount: String): Account? {
        return inMemoryUserProvider.users[userId]?.user?.accounts?.find { it.label == labelAccount }
    }

    override fun findAccountById(accountId: Long): Account? {
        for(user in inMemoryUserProvider.users.values.map { it.user }) {
            for(account in user.accounts) {
                if(account.id == accountId) {
                    return account
                }
            }
        }
        return null
    }

    override fun remove(targetCategory: Tag) {
        tags.remove(targetCategory)
        inMemoryUserProvider.users.values.map { it.user }.forEach {
            it.tags.removeIf { tag -> tag.id == targetCategory.id }
        }
    }

    override fun deleteAccountByID(accountID: Long) {
        inMemoryUserProvider.users.values.map { it.user }.forEach {
            it.removeAccount(accountID)
        }
    }

    override fun saveAllSheets(transactions: List<Transaction>) {
        this.transactions.addAll(transactions)
    }

    override fun deleteAllSheetsById(sheetIds: List<Long>) {
        this.transactions.removeAll { it.id in sheetIds }
    }

    override fun findSheetByID(sheetID: Long): Transaction? {
        return this.transactions.find { it.id == sheetID }
    }

    override fun save(transaction: Transaction): Transaction? {
        return if(this.transactions.add(transaction)) transaction else null
    }

    override fun save(account: Account): Account? {
        return if(this.accounts.add(account)) account else null
    }

    override fun findAccountWithSheetByLabelAndUser(label: String, userId: UserId): Account? {
        return inMemoryUserProvider.users[userId]?.user?.accounts?.find { it.label == label }
    }
}

class InMemoryUserRepository (
    private val inMemoryUserProvider: InMemoryUserProvider
): UserRepository {
    private val users = mutableSetOf<User>()
    private val random = Random()

    override fun findUserById(userId: UserId): User? {
        return inMemoryUserProvider.users[userId]?.user
    }

    override fun findUserByIdWithAccounts(userId: UserId): User? {
        return inMemoryUserProvider.users[userId]?.user
    }

    override fun findByPseudonym(pseudonym: String): User? {
        return inMemoryUserProvider.users.values.find { it.user.username == pseudonym }?.user
    }

    override fun findByPseudonymWithEncodedPassword(pseudonym: String): UserWithPassword? {
        return inMemoryUserProvider.users.values.find { it.user.username == pseudonym }
    }

    override fun create(user: UserWithPassword): User? {
        if(inMemoryUserProvider.users.put(user.user.id, UserWithPassword(user.user, user.password)) == null) return null
        return user.user
    }

    override fun register(username: String, email: String, password: Password): User? {
        val element = User(id = UserId(random.nextLong()), username = username, email = email)
        inMemoryUserProvider.users[element.id] = UserWithPassword(element, password)
        return element
    }

    override fun upsert(user: User): User? {
        val userFound = inMemoryUserProvider.users[user.id] ?: users.add(user)
        users.remove(userFound)
        users.add(user)
        return user
    }
}

class InMemoryAccountRepository: AccountRepository {

    private val accounts = mutableListOf<Account>()

    override fun editFromAnother(account: Account): Account {
        accounts.removeIf { it.id == account.id }
        accounts.add(account)
        return account
    }

    override fun getLastSheetPosition(accountId: Long): Int {
        return 0
    }

    override fun save(ownerId: UserId, account: Account): Account? {
        accounts.add(account)
        return account
    }

    override fun findAccountByIdWithTransactions(accountId: Long): Account? {
        return accounts.find { it.id == accountId }
    }

    override fun deleteAccountById(accountId: Long) {
        accounts.removeIf { it.id == accountId }
    }

    override fun upsert(account: Account): Account {
        accounts.removeIf { it.id == account.id }
        accounts.add(account)
        return account
    }

}

class InMemoryUserProvider {
    val users = mutableMapOf<UserId, UserWithPassword>()
}