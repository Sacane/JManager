package fr.sacane.jmanager.domain.utils

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Nested

class ResultTest {

    @Nested
    inner class ResultStateTests {
        @Test
        fun `OK should be success`() {
            assertTrue(ResultState.OK.isSuccess())
            assertFalse(ResultState.OK.isFailure())
        }

        @Test
        fun `non-OK states should be failure`() {
            assertFalse(ResultState.INVALID.isSuccess())
            assertTrue(ResultState.INVALID.isFailure())
            assertFalse(ResultState.NOT_FOUND.isSuccess())
            assertTrue(ResultState.NOT_FOUND.isFailure())
            assertFalse(ResultState.UNAUTHORIZED.isSuccess())
            assertTrue(ResultState.UNAUTHORIZED.isFailure())
        }

        @Test
        fun `all states should have correct codes`() {
            assertEquals(0, ResultState.OK.code)
            assertEquals(1, ResultState.TIMEOUT.code)
            assertEquals(2, ResultState.INVALID.code)
            assertEquals(3, ResultState.FORBIDDEN.code)
            assertEquals(4, ResultState.NOT_FOUND.code)
            assertEquals(5, ResultState.UNAUTHORIZED.code)
        }
    }

    @Nested
    inner class FactoryFunctionsTests {
        @Test
        fun `success with entity should create OK result`() {
            val result = success("test")

            assertTrue(result.isSuccess())
            assertFalse(result.isFailure())
            assertEquals(ResultState.OK, result.status)
        }

        @Test
        fun `success without entity should create OK result`() {
            val result = success()

            assertTrue(result.isSuccess())
            assertFalse(result.isFailure())
            assertEquals(ResultState.OK, result.status)
        }

        @Test
        fun `invalid without message should create INVALID result`() {
            val result = invalid<String>()

            assertFalse(result.isSuccess())
            assertTrue(result.isFailure())
            assertEquals(ResultState.INVALID, result.status)
        }

        @Test
        fun `invalid with message should create INVALID result with message`() {
            val message = "Invalid data"
            val result = invalid<String>(message)

            assertFalse(result.isSuccess())
            assertTrue(result.isFailure())
            assertEquals(ResultState.INVALID, result.status)
            assertEquals(message, result.message)
        }

        @Test
        fun `notFound should create NOT_FOUND result with message`() {
            val message = "Resource not found"
            val result = notFound<String>(message)

            assertFalse(result.isSuccess())
            assertTrue(result.isFailure())
            assertEquals(ResultState.NOT_FOUND, result.status)
            assertEquals(message, result.message)
        }

        @Test
        fun `forbidden should create FORBIDDEN result with message`() {
            val message = "Access forbidden"
            val result = forbidden<String>(message)

            assertFalse(result.isSuccess())
            assertTrue(result.isFailure())
            assertEquals(ResultState.FORBIDDEN, result.status)
            assertEquals(message, result.message)
        }

        @Test
        fun `timeout should create TIMEOUT result with message`() {
            val message = "Request timeout"
            val result = timeout<String>(message)

            assertFalse(result.isSuccess())
            assertTrue(result.isFailure())
            assertEquals(ResultState.TIMEOUT, result.status)
            assertEquals(message, result.message)
        }

        @Test
        fun `failure should create result with custom state and message`() {
            val message = "User not found"
            val result = failure<String>(ResultState.USER_NOT_FOUND, message)

            assertFalse(result.isSuccess())
            assertTrue(result.isFailure())
            assertEquals(ResultState.USER_NOT_FOUND, result.status)
            assertEquals(message, result.message)
        }

        @Test
        fun `unauthorized companion function should create UNAUTHORIZED result`() {
            val message = "Unauthorized access"
            val result = Result.unauthorized<String>(message)

            assertFalse(result.isSuccess())
            assertTrue(result.isFailure())
            assertEquals(ResultState.UNAUTHORIZED, result.status)
            assertEquals(message, result.message)
        }
    }

    @Nested
    inner class OnSuccessTests {
        @Test
        fun `onSuccess should execute consumer when result is success with data`() {
            var executed = false
            var receivedValue = ""

            success("test").onSuccess { value ->
                executed = true
                receivedValue = value
            }

            assertTrue(executed)
            assertEquals("test", receivedValue)
        }

        @Test
        fun `onSuccess should not execute consumer when result is failure`() {
            var executed = false

            invalid<String>("error").onSuccess {
                executed = true
            }

            assertFalse(executed)
        }

        @Test
        fun `onSuccess should not execute consumer when data is null`() {
            var executed = false
            val result = Result<String>(ResultState.OK, data = null)

            result.onSuccess {
                executed = true
            }

            assertFalse(executed)
        }

        @Test
        fun `onSuccess should return this for chaining`() {
            val result = success("test")
            val returned = result.onSuccess { }

            assertSame(result, returned)
        }
    }

    @Nested
    inner class MapTests {
        @Test
        fun `map should transform data when result is success`() {
            val result = success(5)
            val mapped = result.map { it * 2 }

            assertTrue(mapped.isSuccess())
            var value = 0
            mapped.onSuccess { value = it }
            assertEquals(10, value)
        }

        @Test
        fun `map should transform to different type`() {
            val result = success(42)
            val mapped = result.map { "Number: $it" }

            assertTrue(mapped.isSuccess())
            var value = ""
            mapped.onSuccess { value = it }
            assertEquals("Number: 42", value)
        }

        @Test
        fun `map should preserve error state when result is failure`() {
            val message = "Original error"
            val result = invalid<Int>(message)
            val mapped = result.map { it * 2 }

            assertFalse(mapped.isSuccess())
            assertTrue(mapped.isFailure())
            assertEquals(ResultState.INVALID, mapped.status)
            assertEquals(message, mapped.message)
        }

        @Test
        fun `map should preserve error when data is null`() {
            val result = Result<String>(ResultState.OK, data = null, error = "No data")
            val mapped = result.map { it.length }

            assertTrue(mapped.isSuccess())
            assertEquals("No data", mapped.message)
        }
    }

    @Nested
    inner class MapNullableTests {
        @Test
        fun `mapNullable should transform data when present`() {
            val result = success("hello")
            val length = result.mapNullable { it?.length ?: 0 }

            assertEquals(5, length)
        }

        @Test
        fun `mapNullable should handle null data`() {
            val result = Result<String>(ResultState.OK, data = null)
            val length = result.mapNullable { it?.length ?: -1 }

            assertEquals(-1, length)
        }

        @Test
        fun `mapNullable should work with failure results`() {
            val result = invalid<String>("error")
            val value = result.mapNullable { it ?: "default" }

            assertEquals("default", value)
        }
    }

    @Nested
    inner class MapNotNullOrFailureTests {
        @Test
        fun `mapNotNullOrFailure should return data when success`() {
            val result = success("test")
            val data = result.mapNotNullOrFailure()

            assertEquals("test", data)
        }

        @Test
        fun `mapNotNullOrFailure should return null when failure`() {
            val result = invalid<String>("error")
            val data = result.mapNotNullOrFailure()

            assertNull(data)
        }

        @Test
        fun `mapNotNullOrFailure should return data even if null when success`() {
            val result = Result<String>(ResultState.OK, data = null)
            val data = result.mapNotNullOrFailure()

            assertNull(data)
        }
    }

    @Nested
    inner class MessageTests {
        @Test
        fun `message should return error message`() {
            val errorMsg = "Something went wrong"
            val result = invalid<String>(errorMsg)

            assertEquals(errorMsg, result.message)
        }

        @Test
        fun `message should return default message for success without explicit error`() {
            val result = success("data")

            assertEquals("This response is not an error", result.message)
        }
    }

    @Nested
    inner class IntegrationTests {
        @Test
        fun `chaining multiple operations should work`() {
            val result = success(10)
                .onSuccess { println("Value: $it") }
                .map { it * 2 }
                .map { it + 5 }

            assertTrue(result.isSuccess())
            var finalValue = 0
            result.onSuccess { finalValue = it }
            assertEquals(25, finalValue)
        }

        @Test
        fun `error should propagate through chain`() {
            val errorMsg = "Initial error"
            val result = invalid<Int>(errorMsg)
                .map { it * 2 }
                .map { it + 5 }

            assertFalse(result.isSuccess())
            assertEquals(errorMsg, result.message)
        }

        @Test
        fun `complex data transformation should work`() {
            data class User(val name: String, val age: Int)
            data class UserDto(val displayName: String)

            val user = User("John", 30)
            val result = success(user)
                .map { UserDto("${it.name} (${it.age})") }

            assertTrue(result.isSuccess())
            var dto: UserDto? = null
            result.onSuccess { dto = it }
            assertEquals("John (30)", dto?.displayName)
        }
    }

    @Nested
    inner class DomainErrorCodesTests {
        @Test
        fun `domain error codes should have correct values`() {
            assertEquals(1001, ResultState.BOOKLET_NOT_FOUND.code)
            assertEquals(1002, ResultState.TRANSACTION_NOT_FOUND.code)
            assertEquals(1003, ResultState.TAG_NOT_FOUND.code)
            assertEquals(1004, ResultState.USER_NOT_FOUND.code)
            assertEquals(2001, ResultState.BOOKLET_LABEL_EXIST.code)
            assertEquals(3001, ResultState.USER_NOT_AUTHENTICATED.code)
            assertEquals(5000, ResultState.TRANSACTION_ENTRY_ERROR.code)
        }

        @Test
        fun `failure with domain error code should work`() {
            val result = failure<String>(ResultState.BOOKLET_NOT_FOUND, "Booklet with id 123 not found")

            assertFalse(result.isSuccess())
            assertTrue(result.isFailure())
            assertEquals(ResultState.BOOKLET_NOT_FOUND, result.status)
            assertEquals(1001, result.status.code)
        }
    }

    @Nested
    inner class FlatMapTests {
        @Test
        fun `flatMap should transform data when result is success`() {
            val result = success(5)
            val mapped = result.flatMap { value -> success(value * 2) }

            assertTrue(mapped.isSuccess())
            var final = 0
            mapped.onSuccess { final = it }
            assertEquals(10, final)
        }

        @Test
        fun `flatMap should propagate original failure`() {
            val message = "Original error"
            val result = invalid<Int>(message)
            val mapped = result.flatMap { success(it * 2) }

            assertFalse(mapped.isSuccess())
            assertTrue(mapped.isFailure())
            assertEquals(ResultState.INVALID, mapped.status)
            assertEquals(message, mapped.message)
        }

        @Test
        fun `flatMap should preserve message when data is null`() {
            val result = Result<Int>(ResultState.OK, data = null, error = "No data")
            val mapped = result.flatMap { success(it * 2) }

            assertTrue(mapped.isSuccess())
            assertEquals("No data", mapped.message)
            var executed = false
            mapped.onSuccess { executed = true }
            assertFalse(executed)
        }

        @Test
        fun `flatMap should propagate mapper failure`() {
            val result = success(3)
            val mapped = result.flatMap { invalid<Int>("mapper error") }

            assertFalse(mapped.isSuccess())
            assertTrue(mapped.isFailure())
            assertEquals(ResultState.INVALID, mapped.status)
            assertEquals("mapper error", mapped.message)
        }
    }
}
