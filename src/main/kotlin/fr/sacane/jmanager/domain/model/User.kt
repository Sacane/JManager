package fr.sacane.jmanager.domain.model

import fr.sacane.jmanager.common.Constants
import fr.sacane.jmanager.common.Hash


class UserId(private val id: Long){
    fun get(): Long = id
}

class Password(private val value: String){

    private fun String.toDigit(code: Int) : Int = this.map { it.code + code }.sum()

    fun get(): String{
        return Hash.coder(value, value.toDigit(Constants.CODE))
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
    fun doesPwdMatch(pwd: String): Boolean = pwd == password.get()

    fun accounts(): List<Account> = accounts.distinct()

}
