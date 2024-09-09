package fr.sacane.jmanager.domain.models

import fr.sacane.jmanager.domain.Hash


@JvmInline
value class UserId(val id: Long?)


class Password(value: String?){
    private lateinit var hashedPassword: ByteArray
    private var fromString: Boolean = true
    private constructor(byteArray: ByteArray): this(byteArray.toString()){
        fromString = false
        hashedPassword = byteArray
    }
    companion object{
        fun fromBytes(byteArray: ByteArray): Password{
            return Password(byteArray)
        }
    }
    init{
        require((!value.isNullOrBlank() && value.isNotEmpty())){
            "Given password is blank or empty"
        }
        if(fromString) hashedPassword = Hash.hash(value)
    }
    fun get(): ByteArray{
        return hashedPassword
    }
    fun matchWith(password2: Password): Boolean {
        return get().contentEquals(password2.get())
    }
}

data class MinimalUserRepresentation(
    val id: UserId = UserId(null),
    val username: String,
    val email: String? = null,
)

class User(
    val id: UserId = UserId(null),
    val username: String,
    val email: String?,
    private val accounts_: MutableList<Account> = mutableListOf(),
    val tags: MutableSet<Tag> = mutableSetOf()
) {

    val accounts: List<Account>
        get() = accounts_
    fun withToken(token: AccessToken): UserToken = UserToken(MinimalUserRepresentation(id, username, email), token)
    fun hasAccount(labelAccount: String): Boolean = accounts.any { labelAccount == it.label }
    override fun toString(): String = "username: $username"

    fun removeAccount(accountID: Long) {
        accounts_.removeIf { it.id == accountID }
    }
    fun addAccount(account: Account) {
        accounts_.add(account)
    }
}

data class UserWithPassword(
    val user: User,
    val password: Password,
)