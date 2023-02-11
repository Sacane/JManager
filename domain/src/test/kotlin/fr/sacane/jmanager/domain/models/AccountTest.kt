package fr.sacane.jmanager.domain.models

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.assertEquals

class AccountTest {

    @Test
    fun `earn or loss should be effective for an account`(){
        val account = Account(null, 100.toDouble(), "courant", mutableListOf())
        account += 20.toDouble()
        account.loss(10.toDouble())
        account.earn(50.toDouble())
        account -= 15.toDouble()
        assertThat(account.amount()).isEqualTo(145.toDouble())
    }

    @Test
    fun `transaction should give and take amount`(){
        val account = Account(null, 100.toDouble(), "courant", mutableListOf())
        val account2 = Account(null, 100.toDouble(), "secondaire", mutableListOf())
        account.transaction(10.toDouble(), account2, true)
        assertEquals(account.amount(), 110.toDouble())
        assertEquals(account2.amount(), 90.toDouble())
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
        val user = User(UserId(1), "johan", "johan.test@test.fr", accounts, pwdUser, CategoryFactory.allDefaultCategories())
        assertThat(user.accounts()).containsOnlyOnce(Account(null, constantValue, "test", mutableListOf()))
    }
}
