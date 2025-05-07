package fr.sacane.jmanager.domain

import fr.sacane.jmanager.domain.models.Amount
import java.util.*

fun String.asTokenUUID(): String = this.replace("Bearer ", "")

object Env {
    const val TOKEN_LIFETIME_IN_MINUTES = 30L
    const val REFRESH_TOKEN_LIFETIME_IN_DAYS = 7L
    const val PROD = "prod"

    val isProd: Boolean
        get() = System.getProperty("JMANAGER_ENV") == PROD
}

fun Long.toAmount(): Amount = Amount(this)