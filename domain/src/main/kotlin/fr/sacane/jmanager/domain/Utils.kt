package fr.sacane.jmanager.domain

import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.text.NumberFormat
import java.util.*

fun String.asTokenUUID(): UUID = UUID.fromString(this.replace("Bearer ", ""))

object Env {
    const val TOKEN_LIFETIME_IN_HOURS = 1L
    const val REFRESH_TOKEN_LIFETIME_IN_DAYS = 7L
    const val TEST = "test"
    const val PROD = "prod"
    const val DEV = "dev"

    val isProd: Boolean
        get() = System.getProperty("JMANAGER_ENV") == PROD
}

//TODO rework this entirely because it sucks
object Hash {
    private val md = MessageDigest.getInstance("SHA-512")
    private val salt: ByteArray
    init{
        val file = this::class.java.classLoader.getResourceAsStream("salt.txt")
        salt = if(file == null) {
            "FJZOIDJ".toByteArray()
        } else {
            file.readAllBytes()
        }
    }

    fun hash(pwd: String): ByteArray{
        md.update(salt)
        return md.digest(pwd.toByteArray(StandardCharsets.UTF_8))
    }

    fun contentEquals(pwd: ByteArray, other: String): Boolean{
        val digest = hash(other)
        return pwd.contentEquals(digest)
    }
}

fun Double.toFrenchFormat(): Double{
    val formatter = NumberFormat.getCurrencyInstance(Locale("fr", "FR"))
    val format = formatter.format(this)
    return format.replace(Regex("[^\\d,]"), "").replace(",", ".").toDouble()
}