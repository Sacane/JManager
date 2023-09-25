package fr.sacane.jmanager.domain.models

import fr.sacane.jmanager.domain.Hash


data class UserId(val id: Long?)


class Password(val value: String?){
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

class User(
    val id: UserId,
    val username: String,
    val email: String?,
    val accounts: MutableList<Account>,
    val password: Password,
    private val categories: MutableList<Category>
){
    fun accounts(): MutableList<Account> = accounts.toMutableList()
    fun categories(): MutableList<Category> = categories.distinct().toMutableList()

    fun withToken(token: AccessToken): UserToken = UserToken(this, token)

    override fun toString(): String {
        return """
            id: $id
            username: $username
        """.trimIndent()
    }
}
