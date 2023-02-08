package fr.sacane.jmanager.domain.models

import fr.sacane.jmanager.domain.Hash


class UserId(private val id: Long){
    fun get(): Long = id //In case there is a business rule for user's ID
}


class Password(val value: String){
    init{
        require(value.isNotBlank() && value.isNotEmpty()){
            "Given password is blank or empty"
        }
    }

    fun get(): ByteArray{
        return Hash.hash(value)
    }

    fun matchWith(password2: Password): Boolean {
        return get().contentEquals(password2.get())
    }
}

class User(
    val id: UserId,
    val username: String,
    val email: String,
    val pseudonym: String,
    private val accounts: MutableList<Account>,
    val password: Password,
    private val categories: MutableList<Category>
){

    fun accounts(): List<Account> = accounts.distinct()
    fun categories(): List<Category> = categories.distinct()

}
