package fr.sacane.jmanager.domain.model

import fr.sacane.jmanager.common.Hash


class UserId(private val id: Long){
    fun get(): Long = id //In case there is a business rule for user's ID
}


class Password(val value: String){

    private var hasher = Hash()

    fun get(): ByteArray{
        return hasher.hash(value)
    }

}

class User(
    val id: UserId,
    val username: String,
    val email: String,
    val pseudonym: String,
    private val accounts: MutableList<Account>,
    val password: Password,
    val categories: MutableList<Category>
){

    fun accounts(): List<Account> = accounts.distinct()
    fun categories(): List<Category> = categories.distinct()

}
