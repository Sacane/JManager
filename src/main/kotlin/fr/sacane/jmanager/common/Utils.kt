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
