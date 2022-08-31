package fr.sacane.jmanager.domain

import fr.sacane.jmanager.domain.model.Password
import fr.sacane.jmanager.domain.model.User
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class UserTest {

    @Test
    fun `password should be encrypt`(){
        val pwd = Password("password")
        assertThat(pwd.get()).isNotEqualTo("password")
    }

    @Test
    fun `password should match`(){
        val pwd = Password("password")
        val pwd2 = Password("password")
        assertThat(pwd.get()).isEqualTo(pwd2.get())
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
        val pwdUser = Password("D5301012000MAMacita")

        val user = User("johan", "johan.test@test.fr", "tester", null, pwdUser)

        assertThat(user.doesPwdMatch(pwd.get())).isTrue
    }

    @Test
    fun `user pwd should not match`(){
        val pwd = Password("D5301012000MAMaCitA")
        val pwdUser = Password("D5301012000MAMacita")
        val user = User("johan", "johan.test@test.fr", "tester", null, pwdUser)

        assertThat(user.doesPwdMatch(pwd.get())).isFalse
    }
}
