package fr.sacane.jmanager.domain

import fr.sacane.jmanager.domain.models.Response
import fr.sacane.jmanager.domain.models.ResponseState
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