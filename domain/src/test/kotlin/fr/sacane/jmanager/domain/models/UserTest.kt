package fr.sacane.jmanager.domain.models

import fr.sacane.jmanager.domain.port.spi.DefaultHasher
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class UserTest {

    @Test
    fun `password with default hash should be verify`() {
        val password = "01012000"
        val password2 = "01023000"
        val hash = DefaultHasher.hash(password)

        assertTrue(DefaultHasher.verify(password, hash))
        assertTrue(DefaultHasher.verify(password2, DefaultHasher.hash(password2)))
    }

}
