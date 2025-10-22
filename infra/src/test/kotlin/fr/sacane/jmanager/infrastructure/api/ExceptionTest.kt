package fr.sacane.jmanager.infrastructure.api

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class ExceptionTest {

    @Test
    fun `ForbiddenException should contain error code and message`() {
        val exception = ForbiddenException(403, "Access forbidden")

        assertEquals(403, exception.errCode)
        assertEquals("Access forbidden", exception.message)
    }

    @Test
    fun `TimeOutException should contain error code and message`() {
        val exception = TimeOutException(408, "Request timeout")

        assertEquals(408, exception.errCode)
        assertEquals("Request timeout", exception.message)
    }

    @Test
    fun `NotFoundException should contain error code and message`() {
        val exception = NotFoundException(404, "Resource not found")

        assertEquals(404, exception.errCode)
        assertEquals("Resource not found", exception.message)
    }

    @Test
    fun `InvalidRequestException should contain error code and message`() {
        val exception = InvalidRequestException(400, "Invalid request")

        assertEquals(400, exception.errCode)
        assertEquals("Invalid request", exception.message)
    }

    @Test
    fun `UnauthorizedRequestException should contain error code and message`() {
        val exception = UnauthorizedRequestException(401, "Unauthorized access")

        assertEquals(401, exception.errCode)
        assertEquals("Unauthorized access", exception.message)
    }

    @Test
    fun `InternalServerErrorException should contain error code and message`() {
        val exception = InternalServerErrorException(500, "Internal server error")

        assertEquals(500, exception.errCode)
        assertEquals("Internal server error", exception.message)
    }

    @Test
    fun `Exceptions should be throwable`() {
        assertThrows(ForbiddenException::class.java) {
            throw ForbiddenException(403, "Test")
        }

        assertThrows(TimeOutException::class.java) {
            throw TimeOutException(408, "Test")
        }

        assertThrows(NotFoundException::class.java) {
            throw NotFoundException(404, "Test")
        }

        assertThrows(InvalidRequestException::class.java) {
            throw InvalidRequestException(400, "Test")
        }

        assertThrows(UnauthorizedRequestException::class.java) {
            throw UnauthorizedRequestException(401, "Test")
        }

        assertThrows(InternalServerErrorException::class.java) {
            throw InternalServerErrorException(500, "Test")
        }
    }

    @Test
    fun `Exception messages should be retrievable`() {
        val forbiddenMsg = "You shall not pass"
        val timeoutMsg = "Too slow"
        val notFoundMsg = "Lost in space"
        val invalidMsg = "Bad input"
        val unauthorizedMsg = "Who are you"
        val serverErrorMsg = "Oops"

        assertEquals(forbiddenMsg, ForbiddenException(403, forbiddenMsg).message)
        assertEquals(timeoutMsg, TimeOutException(408, timeoutMsg).message)
        assertEquals(notFoundMsg, NotFoundException(404, notFoundMsg).message)
        assertEquals(invalidMsg, InvalidRequestException(400, invalidMsg).message)
        assertEquals(unauthorizedMsg, UnauthorizedRequestException(401, unauthorizedMsg).message)
        assertEquals(serverErrorMsg, InternalServerErrorException(500, serverErrorMsg).message)
    }
}

