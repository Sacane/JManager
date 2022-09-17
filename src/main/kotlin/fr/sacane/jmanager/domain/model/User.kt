package fr.sacane.jmanager.domain.model

import fr.sacane.jmanager.common.Constants
import fr.sacane.jmanager.common.Hash


class UserId(private val id: Long){
    fun get(): Long = id //In case there is a business rule for ID
}


class Password(val value: String){

    private var hasher = Hash()

    fun get(): String{
        return hasher.hash(value)
    }

    fun matchWith(other: String): Boolean{
        return Hash.verify(other, value)
    }
    fun cryptMatchWith(other: String): Boolean{
        return Hash.verify(other, value.toByteArray())
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

    fun pwdCryptedMatchWith(pwd: String): Boolean = password.cryptMatchWith(pwd)

    fun accounts(): List<Account> = accounts.distinct()

}
