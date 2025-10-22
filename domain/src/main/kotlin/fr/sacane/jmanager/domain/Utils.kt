package fr.sacane.jmanager.domain

import fr.sacane.jmanager.domain.models.Amount
import java.util.UUID

fun String.asTokenUUID(): String = this.replace("Bearer ", "")

object Env {
    const val TOKEN_LIFETIME_IN_MINUTES = 30L
    const val REFRESH_TOKEN_LIFETIME_IN_DAYS = 7L
}

fun Long.toAmount(): Amount = Amount(this)

fun String.toUUID(): UUID = UUID.fromString(this)

fun List<String>.toUUIDs(): List<UUID> = this.map { it.toUUID() }