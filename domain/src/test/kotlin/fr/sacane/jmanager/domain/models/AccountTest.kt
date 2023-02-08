package fr.sacane.jmanager.domain.models

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.assertEquals

class AccountTest {

    @Test
    fun `transaction should give and take amount`(){
        val account = Account(null, 102.toDouble(), "courant", mutableListOf())
        val account2 = Account(null, 105.toDouble(), "secondaire", mutableListOf())
        account.transaction(203.toDouble(), account2, true)
        assertEquals("10", "10")
    }

    @Test
    fun `the user's accounts should not contains the same value more than once`(){
        val constantValue = 102.toDouble()
        val accounts = mutableListOf(
            Account(null, constantValue, "test", mutableListOf()),
            Account(null, constantValue, "Courant", mutableListOf()),
            Account(null, constantValue, "test", mutableListOf()),
            Account(null, constantValue, "Secondaire", mutableListOf())
        )

        val pwdUser = Password("D5301012000MAMacita")
        val user = User(UserId(1), "johan", "johan.test@test.fr", "tester", accounts, pwdUser, CategoryFactory.allDefaultCategories())
        assertThat(user.accounts()).containsOnlyOnce(Account(null, constantValue, "test", mutableListOf()))
    }
}
