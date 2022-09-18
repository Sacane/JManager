package fr.sacane.jmanager.domain

import fr.sacane.jmanager.domain.model.Account
import fr.sacane.jmanager.domain.model.Sheet
import org.junit.jupiter.api.Test
import org.assertj.core.api.Assertions.assertThat

class AccountTest {




    @Test
    fun `transaction should give and take amount`(){
        val account = Account(null, 102.toDouble(), "courant", mutableListOf())
        val account2 = Account(null, 105.toDouble(), "secondaire", mutableListOf())

        account.transaction(203.toDouble(), account2, true)

        assertThat(account.amount()).isEqualTo(305.0)
        assertThat(account2.amount()).isEqualTo(-98.0)
    }
}
