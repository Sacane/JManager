package fr.sacane.jmanager.domain

import fr.sacane.jmanager.domain.utils.Response
import fr.sacane.jmanager.domain.utils.ResponseState
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

interface AuthenticationTest {
    val action: List<Response<out Any>>

    @Test
    fun badAuthTest() {
        action.forEach {
            assertEquals(ResponseState.UNAUTHORIZED, it.status)
        }
    }
}