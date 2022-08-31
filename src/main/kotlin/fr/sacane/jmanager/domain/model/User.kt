package fr.sacane.jmanager.domain.model

data class User(
    val username: String,
    val email: String,
    val pseudonym: String,
    val accounts: MutableList<Account>?
)
