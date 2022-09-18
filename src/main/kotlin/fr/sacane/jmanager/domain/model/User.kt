package fr.sacane.jmanager.domain.model

import fr.sacane.jmanager.common.Constants
import fr.sacane.jmanager.common.Hash


class UserId(private val id: Long){
    fun get(): Long = id //In case there is a business rule for user's ID
}


class Password(val value: String){

    private var hasher = Hash()

    fun get(): String{
        return hasher.hash(value)
    }

    fun matchWith(other: String): Boolean{
        return Hash.verify(other, value)
    }

}

class User(
    val id: UserId,
    val username: String,
    val email: String,
    val pseudonym: String,
    private val accounts: MutableList<Account>,
    val password: Password,
){
    fun pwdMatchWith(pwd: String): Boolean = password.matchWith(pwd)

    fun accounts(): List<Account> = accounts.distinct()

}
