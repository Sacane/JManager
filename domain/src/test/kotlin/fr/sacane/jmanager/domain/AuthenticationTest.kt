package fr.sacane.jmanager.domain

import fr.sacane.jmanager.domain.utils.Result
import fr.sacane.jmanager.domain.utils.ResultState
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

interface AuthenticationTest {
    val action: List<Result<out Any>>

    @Test
    fun badAuthTest() {
        action.forEach {
            assertEquals(ResultState.UNAUTHORIZED, it.status)
        }
    }
}