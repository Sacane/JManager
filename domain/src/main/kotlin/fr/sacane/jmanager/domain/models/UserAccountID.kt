package fr.sacane.jmanager.domain.models

data class UserAccountID(
    val userId: UserId,
    val accountID: Long,
    val token: AccessToken
)
