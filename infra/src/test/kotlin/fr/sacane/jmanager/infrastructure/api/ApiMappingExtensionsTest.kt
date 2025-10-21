package fr.sacane.jmanager.infrastructure.api

import fr.sacane.jmanager.domain.models.*
import fr.sacane.jmanager.domain.models.transaction.Transaction
import fr.sacane.jmanager.domain.utils.ResultState
import fr.sacane.jmanager.domain.utils.failure
import fr.sacane.jmanager.domain.utils.success
import fr.sacane.jmanager.infrastructure.api.tag.ColorDTO
import fr.sacane.jmanager.infrastructure.api.tag.TagDTO
import fr.sacane.jmanager.infrastructure.api.transaction.TransactionResult
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.http.HttpStatus
import java.awt.Color
import java.math.BigDecimal
import java.time.LocalDate

class ApiMappingExtensionsTest {

    @Test
    fun `Booklet toDTO should convert booklet to AccountDTO`() {
        val transaction = Transaction(
            id = 1L,
            label = "Test",
            date = LocalDate.now(),
            amount = 100.toAmount(),
            isIncome = true,
            tag = null
        )
        val booklet = Booklet(
            amount = 1000.toAmount(),
            labelAccount = "Main Account",
            id = 5L
        )
        booklet.addTransaction(transaction)

        val dto = booklet.toDTO()

        assertEquals(5L, dto.id)
        assertEquals(BigDecimal("1100.00"), dto.amount)
        assertEquals("Main Account", dto.labelAccount)
        assertEquals("€", dto.currency)
        assertEquals(1, dto.transactions?.size)
    }

    @Test
    fun `Booklet toDTO should throw exception when id is null`() {
        val booklet = Booklet(
            amount = 1000.toAmount(),
            labelAccount = "No ID Account",
            id = null
        )

        val exception = assertThrows<InternalServerErrorException> {
            booklet.toDTO()
        }

        assertEquals(111, exception.errCode)
        assertTrue(exception.message.contains("null"))
    }

    @Test
    fun `Transaction toDTO should convert transaction correctly`() {
        val tag = Tag(
            label = "Shopping",
            id = 1L,
            color = Color.RED,
            isDefault = false
        )
        val transaction = Transaction(
            id = 10L,
            label = "Buy shoes",
            date = LocalDate.of(2024, 6, 15),
            amount = 150.toAmount(),
            isIncome = false,
            tag = tag
        )

        val dto = transaction.toDTO()

        assertEquals(10L, dto.id)
        assertEquals("Buy shoes", dto.label)
        assertEquals(BigDecimal("150.00"), dto.value)
        assertEquals("€", dto.currency)
        assertFalse(dto.isIncome)
        assertEquals(LocalDate.of(2024, 6, 15), dto.date)
        assertNotNull(dto.tagDTO)
        assertEquals("Shopping", dto.tagDTO?.label)
    }

    @Test
    fun `Transaction toDTO should handle transaction without tag`() {
        val transaction = Transaction(
            id = 11L,
            label = "No tag transaction",
            date = LocalDate.now(),
            amount = 50.toAmount(),
            isIncome = true,
            tag = null
        )

        val dto = transaction.toDTO()

        assertEquals("No tag transaction", dto.label)
        assertNull(dto.tagDTO)
    }

    @Test
    fun `TransactionResult toModel should convert DTO to Transaction`() {
        val colorDTO = ColorDTO(red = 255, green = 0, blue = 0)
        val tagDTO = TagDTO(
            tagId = 1L,
            label = "Food",
            isDefault = false,
            colorDTO = colorDTO
        )
        val transactionResult = TransactionResult(
            id = 20L,
            label = "Restaurant",
            value = BigDecimal("75.50"),
            currency = "€",
            isIncome = false,
            date = LocalDate.of(2024, 7, 10),
            tagDTO = tagDTO,
            isPreview = false
        )

        val transaction = transactionResult.toModel()

        assertEquals(20L, transaction.id)
        assertEquals("Restaurant", transaction.label)
        assertEquals(75.50.toAmount(), transaction.amount)
        assertFalse(transaction.isIncome)
        assertEquals(LocalDate.of(2024, 7, 10), transaction.date)
        assertNotNull(transaction.tag)
        assertEquals("Food", transaction.tag?.label)
        assertEquals(Color(255, 0, 0), transaction.tag?.color)
    }

    @Test
    fun `TransactionResult toModel should use default Aucune tag when tagDTO is null`() {
        val transactionResult = TransactionResult(
            id = 21L,
            label = "No tag",
            value = BigDecimal("100.00"),
            currency = "€",
            isIncome = true,
            date = LocalDate.now(),
            tagDTO = null,
            isPreview = false
        )

        val transaction = transactionResult.toModel()

        assertNotNull(transaction.tag)
        assertEquals("Aucune", transaction.tag?.label)
        assertTrue(transaction.tag?.isDefault ?: false)
    }

    @Test
    fun `User toDTO should convert user correctly`() {
        val user = User(
            id = UserId(123L),
            username = "john_doe",
            email = "john@example.com"
        )

        val dto = user.toDTO()

        assertEquals(123L, dto.id)
        assertEquals("john_doe", dto.username)
        assertEquals("john@example.com", dto.email)
    }

    @Test
    fun `Long id extension should create UserId`() {
        val userId = 456L.id()

        assertEquals(456L, userId.value)
    }

    @Test
    fun `Result toHttpResponse should return OK for successful result`() {
        val result = success("Test data")

        val response = result.toHttpResponse()

        assertEquals(HttpStatus.OK, response.statusCode)
        assertEquals("Test data", response.body)
    }

    @Test
    fun `Result toHttpResponse should throw NotFoundException for NOT_FOUND status`() {
        val result = failure<String>(ResultState.NOT_FOUND, "Resource not found")

        val exception = assertThrows<NotFoundException> {
            result.toHttpResponse()
        }

        assertEquals(ResultState.NOT_FOUND.code, exception.errCode)
        assertEquals("Resource not found", exception.message)
    }

    @Test
    fun `Result toHttpResponse should throw InvalidRequestException for INVALID status`() {
        val result = failure<String>(ResultState.INVALID, "Invalid request")

        val exception = assertThrows<InvalidRequestException> {
            result.toHttpResponse()
        }

        assertEquals(ResultState.INVALID.code, exception.errCode)
        assertEquals("Invalid request", exception.message)
    }

    @Test
    fun `Result toHttpResponse should throw ForbiddenException for FORBIDDEN status`() {
        val result = failure<String>(ResultState.FORBIDDEN, "Access forbidden")

        val exception = assertThrows<ForbiddenException> {
            result.toHttpResponse()
        }

        assertEquals(ResultState.FORBIDDEN.code, exception.errCode)
        assertEquals("Access forbidden", exception.message)
    }

    @Test
    fun `Result toHttpResponse should throw TimeOutException for TIMEOUT status`() {
        val result = failure<String>(ResultState.TIMEOUT, "Request timeout")

        val exception = assertThrows<TimeOutException> {
            result.toHttpResponse()
        }

        assertEquals(ResultState.TIMEOUT.code, exception.errCode)
        assertEquals("Request timeout", exception.message)
    }

    @Test
    fun `Result toHttpResponse should throw UnauthorizedRequestException for UNAUTHORIZED status`() {
        val result = failure<String>(ResultState.UNAUTHORIZED, "Unauthorized")

        val exception = assertThrows<UnauthorizedRequestException> {
            result.toHttpResponse()
        }

        assertEquals(ResultState.UNAUTHORIZED.code, exception.errCode)
        assertEquals("Unauthorized", exception.message)
    }

    @Test
    fun `Result toHttpResponse should throw InternalServerErrorException for INTERNAL_SERVER_ERROR status`() {
        val result = failure<String>(ResultState.INTERNAL_SERVER_ERROR, "Server error")

        val exception = assertThrows<InternalServerErrorException> {
            result.toHttpResponse()
        }

        assertEquals(ResultState.INTERNAL_SERVER_ERROR.code, exception.errCode)
        assertEquals("Server error", exception.message)
    }

    @Test
    fun `Color toDTO should convert AWT Color to ColorDTO`() {
        val color = Color(128, 64, 255)

        val dto = color.toDTO()

        assertEquals(128, dto.red)
        assertEquals(64, dto.green)
        assertEquals(255, dto.blue)
    }

    @Test
    fun `ColorDTO asAwtColor should convert to AWT Color`() {
        val colorDTO = ColorDTO(red = 200, green = 100, blue = 50)

        val color = colorDTO.asAwtColor()

        assertEquals(200, color.red)
        assertEquals(100, color.green)
        assertEquals(50, color.blue)
    }

    @Test
    fun `Tag toDTO should convert tag correctly`() {
        val tag = Tag(
            label = "Transport",
            id = 42L,
            color = Color.BLUE,
            isDefault = true
        )

        val dto = tag.toDTO()

        assertEquals(42L, dto.tagId)
        assertEquals("Transport", dto.label)
        assertTrue(dto.isDefault)
        assertEquals(0, dto.colorDTO.red)
        assertEquals(0, dto.colorDTO.green)
        assertEquals(255, dto.colorDTO.blue)
    }

    @Test
    fun `TagDTO toDomain should convert to Tag model`() {
        val colorDTO = ColorDTO(red = 255, green = 128, blue = 0)
        val tagDTO = TagDTO(
            tagId = 99L,
            label = "Custom Tag",
            isDefault = false,
            colorDTO = colorDTO
        )

        val tag = tagDTO.toDomain()

        assertEquals(99L, tag.id)
        assertEquals("Custom Tag", tag.label)
        assertFalse(tag.isDefault)
        assertEquals(Color(255, 128, 0), tag.color)
    }

    @Test
    fun `Result toHttpResponse should handle all NOT_FOUND variants`() {
        val notFoundStates = listOf(
            ResultState.TAG_NOT_FOUND,
            ResultState.USER_NOT_FOUND,
            ResultState.BOOKLET_NOT_FOUND,
            ResultState.TRANSACTION_NOT_FOUND
        )

        notFoundStates.forEach { state ->
            val result = failure<String>(state, "Not found")
            assertThrows<NotFoundException> {
                result.toHttpResponse()
            }
        }
    }

    @Test
    fun `Result toHttpResponse should handle all INVALID variants`() {
        val invalidStates = listOf(
            ResultState.BAD_REQUEST,
            ResultState.INFRASTRUCTURE_ERROR,
            ResultState.REGISTRATION_ERROR,
            ResultState.TRANSACTION_ENTRY_ERROR
        )

        invalidStates.forEach { state ->
            val result = failure<String>(state, "Invalid")
            assertThrows<InvalidRequestException> {
                result.toHttpResponse()
            }
        }
    }

    @Test
    fun `Result toHttpResponse should handle all UNAUTHORIZED variants`() {
        val unauthorizedStates = listOf(
            ResultState.USER_NOT_AUTHENTICATED,
            ResultState.PASSWORD_NOT_MATCH
        )

        unauthorizedStates.forEach { state ->
            val result = failure<String>(state, "Unauthorized")
            assertThrows<UnauthorizedRequestException> {
                result.toHttpResponse()
            }
        }
    }
}
