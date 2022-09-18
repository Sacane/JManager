package fr.sacane.jmanager.common

import com.toxicbakery.bcrypt.Bcrypt
import fr.sacane.jmanager.domain.model.Account
import java.nio.charset.StandardCharsets
import java.security.MessageDigest


class Hash() {
    private val salt = Constants.SALT.toByteArray()
    private val md = MessageDigest.getInstance("SHA-512")

    init{
        md.update(salt)
    }

    fun hash(pwd: String): String{
        return String(md.digest(pwd.toByteArray(StandardCharsets.UTF_8)))
    }


    companion object {

        fun hash(pass:String): ByteArray{
            return Bcrypt.hash(pass, Constants.CODE)
        }
        fun verify(given: String, expected: String): Boolean{
            return Bcrypt.verify(given, Bcrypt.hash(expected, Constants.CODE))
        }
        fun verify(given: String, expected: ByteArray): Boolean{
            return Bcrypt.verify(given, expected)
        }
    }
}

