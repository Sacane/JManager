package fr.sacane.jmanager.domain

import fr.sacane.jmanager.domain.models.Amount

fun String.asTokenUUID(): String = this.replace("Bearer ", "")

object Env {
    const val TOKEN_LIFETIME_IN_MINUTES = 30L
    const val REFRESH_TOKEN_LIFETIME_IN_DAYS = 7L
}

fun Long.toAmount(): Amount = Amount(this)