package fr.sacane.jmanager.common

import at.favre.lib.crypto.bcrypt.BCrypt
import at.favre.lib.crypto.bcrypt.LongPasswordStrategies
import com.toxicbakery.bcrypt.Bcrypt
import fr.sacane.jmanager.domain.model.Account
import fr.sacane.jmanager.domain.model.Password
import java.security.SecureRandom
import kotlin.math.exp

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

class Hash{


    companion object {

        fun hash(pass:String): String{
            return Bcrypt.hash(pass, Constants.CODE).toString()
        }
        fun verify(given: String, expected: String): Boolean{
            return Bcrypt.verify(given, Bcrypt.hash(expected, Constants.CODE))
        }
        fun verify(given: String, expected: ByteArray): Boolean{
            return Bcrypt.verify(given, expected)
        }
    }
}

