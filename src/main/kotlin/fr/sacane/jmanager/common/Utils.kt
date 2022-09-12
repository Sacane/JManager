package fr.sacane.jmanager.common

import fr.sacane.jmanager.domain.model.Account

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
        private fun String.toDigit(salt:String, code: Int): Int{
            return salt.map { it.code + code}.sum()
        }
        fun coder(pass: String, code: Int): String{
            val hashBuilder = StringBuilder()
            var c: Char
            pass.forEach { char ->
                c = char
                var value = c.code.toLong()
                value += code
                value *= pass.length
                value += pass.toDigit(pass, code)
                value %= 255
                c = value.toInt().toChar()
                hashBuilder.append(c)
            }
            return hashBuilder.toString()
        }
    }

}

