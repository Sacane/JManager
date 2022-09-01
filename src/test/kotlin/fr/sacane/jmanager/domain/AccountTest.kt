package fr.sacane.jmanager.domain

import fr.sacane.jmanager.common.transaction
import fr.sacane.jmanager.domain.model.Account
import org.junit.jupiter.api.Test
import org.assertj.core.api.Assertions.assertThat

class AccountTest {

    @Test
    fun `transaction should give and take amount`(){
        val account = Account(102.toDouble(), "courant", emptyList())
        val account2 = Account(105.toDouble(), "secondaire", emptyList())

        account.transaction(203.toDouble(), account2, true)

        assertThat(account.amount()).isEqualTo(305.0)
        assertThat(account2.amount()).isEqualTo(-98.0)
    }
}
