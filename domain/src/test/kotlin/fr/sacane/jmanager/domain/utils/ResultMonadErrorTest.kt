package fr.sacane.jmanager.domain.utils

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ResultMonadErrorTest {

    @Test
    fun `failure with state and message creates typed domain error`() {
        val result = failure<String>(ResultState.BOOKLET_NOT_FOUND, "booklet missing")

        assertTrue(result.isFailure())
        assertNotNull(result.errorInfo)
        assertEquals(ResultState.BOOKLET_NOT_FOUND.code, result.errorInfo?.code)
        assertEquals(ErrorCatalog.keyForCode(ResultState.BOOKLET_NOT_FOUND.code), result.errorInfo?.key)
        assertEquals("booklet missing", result.errorInfo?.detail)
        assertEquals("booklet missing", result.message)
    }

    @Test
    fun `failure with explicit domain error preserves explicit key`() {
        val customError = DomainError(
            code = ResultState.TRANSACTION_NOT_FOUND.code,
            key = "domain.transaction.not_found",
            detail = "transaction missing",
        )
        val result = failure<String>(ResultState.TRANSACTION_NOT_FOUND, customError)

        assertEquals(customError, result.errorInfo)
        assertEquals("transaction missing", result.message)
    }

    @Test
    fun `map and flatMap preserve monadic error payload`() {
        val customError = DomainError(
            code = ResultState.USER_UNAUTHORIZED.code,
            key = "domain.user.unauthorized",
            detail = "unauthorized",
        )
        val failed = failure<String>(ResultState.USER_UNAUTHORIZED, customError)

        val mapped = failed.map { it.length }
        val flatMapped = failed.flatMap { success(it.length) }

        assertEquals(customError, mapped.errorInfo)
        assertEquals(customError, flatMapped.errorInfo)
    }
}
