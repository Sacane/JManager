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
import java.util.UUID

class ApiMappingExtensionsTest {

    @Test
    fun `Booklet toDTO should convert booklet to BookletDTO`() {
        val transaction = Transaction(
            id = UUID.randomUUID(),
            label = "Test",
            date = LocalDate.now(),
            amount = 100.toAmount(),
            isIncome = true,
            tag = null
        )
        val bookletId = UUID.randomUUID()
        val booklet = Booklet(
            amount = 1000.toAmount(),
            label = "Main Account",
            id = bookletId
        )
        booklet.addTransaction(transaction)

        val dto = booklet.toDTO()

        assertEquals(bookletId.toString(), dto.id)
        assertEquals(BigDecimal("1100.00"), dto.amount)
        assertEquals("Main Account", dto.label)
        assertEquals("€", dto.currency)
        assertEquals(1, dto.transactions?.size)
    }

    @Test
    fun `Booklet toDTO should throw exception when id is null`() {
        val booklet = Booklet(
            amount = 1000.toAmount(),
            label = "No ID Account",
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
            id = UUID.randomUUID(),
            color = Color.RED,
            isDefault = false
        )
        val trId = UUID.randomUUID()
        val transaction = Transaction(
            id = trId,
            label = "Buy shoes",
            date = LocalDate.of(2024, 6, 15),
            amount = 150.toAmount(),
            isIncome = false,
            tag = tag
        )

        val dto = transaction.toDTO()

        assertEquals(trId.toString(), dto.id)
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
            id = UUID.randomUUID(),
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
            tagId = UUID.randomUUID().toString(),
            label = "Food",
            isDefault = false,
            colorDTO = colorDTO
        )
        val trId = UUID.randomUUID()
        val transactionResult = TransactionResult(
            id = trId.toString(),
            label = "Restaurant",
            value = BigDecimal("75.50"),
            currency = "€",
            isIncome = false,
            date = LocalDate.of(2024, 7, 10),
            tagDTO = tagDTO,
            isPreview = false
        )

        val transaction = transactionResult.toModel()

        assertEquals(trId, transaction.id)
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
            id = UUID.randomUUID().toString(),
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
        val userId = UUID.randomUUID()
        val user = User(
            id = UserId(userId),
            username = "john_doe",
            email = "john@example.com"
        )

        val dto = user.toDTO()

        assertEquals(userId.toString(), dto.id)
        assertEquals("john_doe", dto.username)
        assertEquals("john@example.com", dto.email)
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
        val tagId = UUID.randomUUID()
        val tag = Tag(
            label = "Transport",
            id = tagId,
            color = Color.BLUE,
            isDefault = true
        )

        val dto = tag.toDTO()

        assertEquals(tagId.toString(), dto.tagId)
        assertEquals("Transport", dto.label)
        assertTrue(dto.isDefault)
        assertEquals(0, dto.colorDTO.red)
        assertEquals(0, dto.colorDTO.green)
        assertEquals(255, dto.colorDTO.blue)
    }

    @Test
    fun `TagDTO toDomain should convert to Tag model`() {
        val colorDTO = ColorDTO(red = 255, green = 128, blue = 0)
        val tagId = UUID.randomUUID()
        val tagDTO = TagDTO(
            tagId = tagId.toString(),
            label = "Custom Tag",
            isDefault = false,
            colorDTO = colorDTO
        )

        val tag = tagDTO.toDomain()

        assertEquals(tagId, tag.id)
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
