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

class MinimalUserRepresentation(
    val id: UserId = UserId(null),
    val username: String,
    val email: String? = null,
)

class User(
    val id: UserId = UserId(null),
    val username: String,
    val email: String?,
    private val accounts_: MutableList<Account> = mutableListOf(),
    val password: Password,
    val tags: MutableSet<Tag> = mutableSetOf(
        "Loisir".asTag(),
        "Repas".asTag(),
        "Autres".asTag(),
        "Mise en epargne".asTag(),
        "Education".asTag(),
        "Cadeaux".asTag(),
        "Remboursement".asTag(),
        "Sante".asTag(),
        "Vie quotidienne".asTag(),
        "Voyage et Transports".asTag(),
        "Revenus".asTag(),
        "Abonnements".asTag()
    )
) {

    val accounts: MutableList<Account>
        get() = accounts_
    fun withToken(token: AccessToken): UserToken = UserToken(MinimalUserRepresentation(id, username, email), token)
    fun hasAccount(labelAccount: String): Boolean = accounts.any { labelAccount == it.label }
    override fun toString(): String = "username: $username"

    fun removeAccount(accountID: Long) {
        accounts.removeIf { it.id == accountID }
    }
}
