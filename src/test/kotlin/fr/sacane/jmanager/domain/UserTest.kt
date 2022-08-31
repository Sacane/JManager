package fr.sacane.jmanager.domain

import fr.sacane.jmanager.domain.model.Password
import fr.sacane.jmanager.domain.model.User
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class UserTest {

    @Test
    fun password_should_be_encrypt(){
        val pwd = Password("password")
        assertThat(pwd.get()).isNotEqualTo("password")
    }

    @Test
    fun password_should_match(){
        val pwd = Password("password")
        val pwd2 = Password("password")
        assertThat(pwd.get()).isEqualTo(pwd2.get())
    }

    @Test
    fun password_should_not_match_even_with_uppercase(){
        val pwd = Password("password")
        val pwd2 = Password("PAsswoRD")
        assertThat(pwd.get()).isNotEqualTo(pwd2.get())
    }

    @Test
    fun user_pwd_should_match(){
        val pwd = Password("D5301012000MAMacita")
        val pwdUser = Password("D5301012000MAMacita")

        val user = User("johan", "johan.test@test.fr", "tester", null, pwdUser)

        assertThat(user.doesPwdMatch(pwd.get())).isTrue
    }

    @Test
    fun user_pwd_should_not_match(){
        val pwd = Password("D5301012000MAMaCitA")
        val pwdUser = Password("D5301012000MAMacita")
        val user = User("johan", "johan.test@test.fr", "tester", null, pwdUser)

        assertThat(user.doesPwdMatch(pwd.get())).isFalse
    }
}
