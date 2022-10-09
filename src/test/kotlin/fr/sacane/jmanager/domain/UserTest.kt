package fr.sacane.jmanager.domain

import com.toxicbakery.bcrypt.Bcrypt
import fr.sacane.jmanager.common.Hash
import fr.sacane.jmanager.domain.model.*
import org.apache.tomcat.util.digester.DocumentProperties.Charset
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class UserTest {

    @Test
    fun `password should match`(){
        assertThat(Hash.verify("password", "password")).isTrue
    }

    @Test
    fun `password should not match even with uppercase`(){
        val pwd = Password("password")
        val pwd2 = Password("PAsswoRD")
        assertThat(pwd.get()).isNotEqualTo(pwd2.get())
    }

    @Test
    fun `user pwd should match with same`(){
        val pwd = Password("D5301012000MAMacita")

        val user = User(UserId(1), "johan", "johan.test@test.fr", "tester", mutableListOf(), pwd, CategoryFactory.allDefaultCategories())

        assertThat(user.pwdMatchWith("D5301012000MAMacita")).isTrue
        val pw2 = Password("D5301012000MAMacita")
        assertThat(pw2.matchWith("D5301012000MAMacita")).isTrue

        val hashed = Hash.hash("DBAEUHABUD")
        assertThat(Bcrypt.verify("DBAEUHABUD", Bcrypt.hash("DBAEUHABUD", 5))).isTrue

    }

    @Test
    fun `wrong pwd should not match`(){
        val pwd = Password("D5301012000MAMaCitA")
        val pwdUser = Password("D5301012000MAMacita")
        val user = User(UserId(1), "johan", "johan.test@test.fr", "tester", mutableListOf(), pwdUser, CategoryFactory.allDefaultCategories())
        assertThat(user.pwdMatchWith("D5301012000MAMacita")).isTrue
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
