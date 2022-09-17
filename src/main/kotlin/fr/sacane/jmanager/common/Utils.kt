package fr.sacane.jmanager.common

import com.toxicbakery.bcrypt.Bcrypt
import fr.sacane.jmanager.domain.model.Account
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
operator fun Account.plusAssign(earned: Double){
    this.earnAmount(earned)
}
operator fun Account.minusAssign(loss: Double){
    this.lossAmount(loss)
}

fun Account.transaction(delta: Double, otherAccount: Account, isEntry: Boolean){
    if(isEntry){
        this += delta
        otherAccount -= delta
    } else {
        this -= delta
        otherAccount += delta
    }
}

class Hash() {
    private var salt = Constants.SALT.toByteArray()
    private var md = MessageDigest.getInstance("SHA-512")

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

