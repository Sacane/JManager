package fr.sacane.jmanager.domain.models

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class UserTest {

    @Test
    fun `Blank or empty password should not be effective`(){
        val message = assertThrows<IllegalArgumentException> {
            Password("")
        }
        val message2 = assertThrows<IllegalArgumentException>{
            Password(" ")
        }
        assertEquals(message.message, "Given password is blank or empty")
        assertEquals(message2.message, "Given password is blank or empty")
    }

    @Test
    fun `password should not match even with uppercase`(){
        val pwd = Password("password")
        val pwd2 = Password("PAsswoRD")
        assertNotEquals(pwd.get(), pwd2.get())
        assertFalse(pwd.matchWith(pwd2))
    }

    @Test
    fun `two same hashed password should match, other should not`(){
        val password = Password("01012000")
        val password2 = Password("01023000")
        val passwordClone = Password("01012000")
        assertFalse(password.matchWith(password2))
        assertTrue(password.matchWith(passwordClone))
        assertTrue(Password.fromBytes(password.get()).matchWith(password))
    }

}
