package fr.sacane.jmanager.domain

import fr.sacane.jmanager.domain.models.*
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class UserTest {


    @Test
    fun `password should not match even with uppercase`(){
        val pwd = Password("password")
        val pwd2 = Password("PAsswoRD")
        assertThat(pwd.get()).isNotEqualTo(pwd2.get())
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

    @Test
    fun `two hashed password should match`(){
        val text = Password("test")
        assertThat(Hash.contentEquals(text.get(), "test")).isTrue
    }

}
