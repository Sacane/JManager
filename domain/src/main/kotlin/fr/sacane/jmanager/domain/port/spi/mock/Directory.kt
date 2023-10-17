package fr.sacane.jmanager.domain.port.spi.mock

import fr.sacane.jmanager.domain.hexadoc.DefaultSource
import fr.sacane.jmanager.domain.models.*
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.Month
import java.util.*

class Directory {

    companion object{
        val sheetInventory = mutableListOf(
            Sheet(0, "Piano", LocalDate.of(2022, Month.DECEMBER, 1), 450.toAmount(), 0.0.toAmount(), 100.0.toAmount(), Category("Fun")),
            Sheet(1, "Salary", LocalDate.now(), 3500.toAmount(), 0.0.toAmount(),100.0.toAmount(), Category("Work")),
            Sheet(2, "SingLessons", LocalDate.now(), 450.toAmount(), 0.0.toAmount(),100.0.toAmount(), Category("Fun")),
            Sheet(3, "Restaurant", LocalDate.of(2022, Month.DECEMBER, 4), 100.toAmount(), 0.0.toAmount(),100.0.toAmount(), Category("Fun")),
            Sheet(4, "Laptop", LocalDate.of(2022, Month.DECEMBER, 31), 450.toAmount(), 0.0.toAmount(), 100.0.toAmount(),Category("Nothing")),
            Sheet(5, "", LocalDate.now(), 450.toAmount(), 0.0.toAmount(), 100.0.toAmount(),Category("Fun")),
            Sheet(6, "Money From testX", LocalDate.now(), 450.toAmount(), 0.0.toAmount(), 100.0.toAmount(),Category("Transaction"))
        )
    }
    private val categories = mutableListOf(
        Category("Fun"),
        Category("Work"),
        Category("Nothing"),
        Category("Transaction")
    )


    private val accountInventory = mutableListOf(
        Account(1, 0.toAmount(), "Principal", sheetInventory)
    )

    private val tokenInventory = mutableMapOf(
        "test1" to AccessToken(UUID.randomUUID(), LocalDateTime.now().plusSeconds(5), UUID.randomUUID()),
        "test2" to AccessToken(UUID.randomUUID(), LocalDateTime.now().plusSeconds(5), UUID.randomUUID()),
        "test3" to AccessToken(UUID.randomUUID(), LocalDateTime.now().plusSeconds(5), UUID.randomUUID()),
        "test4" to AccessToken(UUID.randomUUID(), LocalDateTime.now().plusSeconds(5), UUID.randomUUID()),
        "test5" to AccessToken(UUID.randomUUID(), LocalDateTime.now().plusSeconds(5), UUID.randomUUID()),
        "test6" to AccessToken(UUID.randomUUID(), LocalDateTime.now().plusSeconds(5), UUID.randomUUID()),
        "test7" to AccessToken(UUID.randomUUID(), LocalDateTime.now().plusSeconds(5), UUID.randomUUID()),
        "test8" to AccessToken(UUID.randomUUID(), LocalDateTime.now().plusSeconds(5), UUID.randomUUID()),
        "test9" to AccessToken(UUID.randomUUID(), LocalDateTime.now().plusSeconds(5), UUID.randomUUID())
    )

    private val userInventory = mutableListOf(
        User(UserId(1L), "test1", "test1.test@test.fr",  accountInventory, Password("01010122321"), categories),
        User(UserId(2L), "test2", "test2.test@test.fr",  accountInventory, Password("01010122332"), categories),
        User(UserId(3L), "test3", "test3.test@test.fr",  accountInventory, Password("01010122323"), categories),
    )



    @DefaultSource
    inner class UserTransactionMock {

        fun findById(userId: UserId): UserToken? {
            val user = userInventory.find { it.id.id == userId.id } ?: return null
            return tokenInventory[user.username]?.let { UserToken(user, it) }
        }

        fun findUserById(userId: UserId): User? {
            TODO("Not yet implemented")
        }

        fun checkUser(pseudonym: String, pwd: Password): UserToken? {
            TODO("Not yet implemented")
        }
        fun findByPseudonym(pseudonym: String): User? {
            return userInventory.find { it.username == pseudonym }
        }
        fun create(user: User): User? {
            return register(user)
        }
        fun register(user: User): User? {
            if(userInventory.find { it.username == user.username || it.id.id == user.id.id } != null) return null
            userInventory.add(user)
            return user
        }
        fun getUserToken(userId: UserId): AccessToken? {
            val user = findById(userId) ?: return null
            return tokenInventory[user.user.username]
        }

        fun upsert(user: User): User? {
            TODO("Not yet implemented")
        }
    }
}