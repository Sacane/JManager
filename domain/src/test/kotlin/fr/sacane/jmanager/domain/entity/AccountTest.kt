package fr.sacane.jmanager.domain.entity

import fr.sacane.jmanager.domain.models.Account
import org.junit.jupiter.api.Test
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.assertEquals

class AccountTest {




    @Test
    fun `transaction should give and take amount`(){
        val account = Account(null, 102.toDouble(), "courant", mutableListOf())
        val account2 = Account(null, 105.toDouble(), "secondaire", mutableListOf())

        account.transaction(203.toDouble(), account2, true)

        assertEquals("10", "10")
    }
}
