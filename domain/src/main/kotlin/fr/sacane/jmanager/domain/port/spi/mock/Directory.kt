package fr.sacane.jmanager.domain.port.spi.mock

import fr.sacane.jmanager.domain.hexadoc.DefaultSource
import fr.sacane.jmanager.domain.models.*
import fr.sacane.jmanager.domain.port.spi.UserTransaction
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

class Directory {
    private val categories = mutableListOf(
        Category("Fun"),
        Category("Work"),
        Category("Nothing"),
        Category("Transaction")
    )
    private val sheetInventory = mutableListOf(
        Sheet(0, "Piano", LocalDate.now(), 450.toDouble(), false, Category("Fun")),
        Sheet(1, "Salary", LocalDate.now(), 3500.toDouble(), true, Category("Work")),
        Sheet(2, "SingLessons", LocalDate.now(), 450.toDouble(), false, Category("Fun")),
        Sheet(3, "Restaurant", LocalDate.now(), 100.toDouble(), false, Category("Fun")),
        Sheet(4, "Laptop", LocalDate.now(), 450.toDouble(), false, Category("Nothing")),
        Sheet(5, "", LocalDate.now(), 450.toDouble(), true, Category("Fun")),
        Sheet(6, "Money From testX", LocalDate.now(), 450.toDouble(), true, Category("Transaction"))
    )

    private val accountInventory = mutableListOf(
        Account(1, 0.toDouble(), "Principal", sheetInventory)
    )

    private val tokenInventory = mutableMapOf(
        "test1" to Token(UUID.randomUUID(), LocalDateTime.now().plusSeconds(5), UUID.randomUUID()),
        "test2" to Token(UUID.randomUUID(), LocalDateTime.now().plusSeconds(5), UUID.randomUUID()),
        "test3" to Token(UUID.randomUUID(), LocalDateTime.now().plusSeconds(5), UUID.randomUUID()),
        "test4" to Token(UUID.randomUUID(), LocalDateTime.now().plusSeconds(5), UUID.randomUUID()),
        "test5" to Token(UUID.randomUUID(), LocalDateTime.now().plusSeconds(5), UUID.randomUUID()),
        "test6" to Token(UUID.randomUUID(), LocalDateTime.now().plusSeconds(5), UUID.randomUUID()),
        "test7" to Token(UUID.randomUUID(), LocalDateTime.now().plusSeconds(5), UUID.randomUUID()),
        "test8" to Token(UUID.randomUUID(), LocalDateTime.now().plusSeconds(5), UUID.randomUUID()),
        "test9" to Token(UUID.randomUUID(), LocalDateTime.now().plusSeconds(5), UUID.randomUUID())
    )

    private val userInventory = mutableListOf(
        User(UserId(1L), "test1", "test1.test@test.fr", "tester1", accountInventory, Password("01010122321"), categories),
        User(UserId(2L), "test2", "test2.test@test.fr", "tester2", accountInventory, Password("01010122332"), categories),
        User(UserId(3L), "test3", "test3.test@test.fr", "tester3", accountInventory, Password("01010122323"), categories),
    )



    @DefaultSource
    inner class UserTransactionMock : UserTransaction {

        override fun findById(userId: UserId): Ticket? {
            val user = userInventory.find { it.id.get() == userId.get() } ?: return null
            return tokenInventory[user.username]?.let { Ticket(user, it) }
        }
        override fun checkUser(pseudonym: String, pwd: Password): Ticket? {
            TODO("Not yet implemented")
        }
        override fun findByPseudonym(pseudonym: String): User? {
            return userInventory.find { it.pseudonym == pseudonym }
        }
        override fun create(user: User): User? {
            return register(user)
        }
        override fun register(user: User): User? {
            if(userInventory.find { it.username == user.username || it.id.get() == user.id.get() } != null) return null
            userInventory.add(user)
            return user
        }
        override fun getUserToken(userId: UserId): Token? {
            val user = findById(userId) ?: return null
            return tokenInventory[user.user?.username]
        }
    }
}