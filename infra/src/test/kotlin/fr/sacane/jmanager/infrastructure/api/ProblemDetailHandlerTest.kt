package fr.sacane.jmanager.infrastructure.api

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus
import org.springframework.web.bind.MissingServletRequestParameterException
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.beans.TypeMismatchException
import java.time.DateTimeException
import org.mockito.Mockito
import fr.sacane.jmanager.domain.models.InvalidCurrencyException

class ProblemDetailHandlerTest {

    private val handler = ProblemDetailHandler()

    @Test
    fun `handle missing parameter returns bad request with code 65`() {
        val ex = MissingServletRequestParameterException("id", "String")
        val resp = handler.handleMissingParams(ex)

        assertThat(resp.statusCode.value()).isEqualTo(HttpStatus.BAD_REQUEST.value())
        val pd = resp.body!!
        assertThat(pd.status).isEqualTo(HttpStatus.BAD_REQUEST.value())
        assertThat(pd.title).isEqualTo("Bad Request")
        assertThat(pd.detail).contains("Missing mandatory parameter")
        assertThat(pd.properties!!["code"]).isEqualTo(65)
    }

    @Test
    fun `method argument type mismatch returns bad request`() {
        val ex = Mockito.mock(MethodArgumentTypeMismatchException::class.java)
        Mockito.`when`(ex.message).thenReturn("Type mismatch for param")
        val resp = handler.handleTypeMismatch(ex)

        assertThat(resp.statusCode.value()).isEqualTo(HttpStatus.BAD_REQUEST.value())
        val pd = resp.body!!
        assertThat(pd.status).isEqualTo(HttpStatus.BAD_REQUEST.value())
        assertThat(pd.title).isEqualTo("Method type mismatch")
        assertThat(pd.detail).contains("Invalid type parameter")
        assertThat(pd.properties!!["code"]).isEqualTo(65)
    }

    @Test
    fun `method argument not valid returns bad request`() {
        val ex = Mockito.mock(MethodArgumentNotValidException::class.java)
        Mockito.`when`(ex.message).thenReturn("Validation failed")
        val resp = handler.handleValidationErrors(ex)

        assertThat(resp.statusCode.value()).isEqualTo(HttpStatus.BAD_REQUEST.value())
        val pd = resp.body!!
        assertThat(pd.status).isEqualTo(HttpStatus.BAD_REQUEST.value())
        assertThat(pd.title).isEqualTo("Method argument not valid")
        assertThat(pd.detail).contains("invalid value")
        assertThat(pd.properties!!["code"]).isEqualTo(65)
    }

    @Test
    fun `invalid currency exception returns bad request with code 144`() {
        val ex = InvalidCurrencyException("nope")
        val resp = handler.onCurrencyException(ex)

        assertThat(resp.statusCode.value()).isEqualTo(HttpStatus.BAD_REQUEST.value())
        val pd = resp.body!!
        assertThat(pd.status).isEqualTo(HttpStatus.BAD_REQUEST.value())
        assertThat(pd.title).isEqualTo("Invalid given currency")
        assertThat(pd.detail).contains("Oops, something went wrong")
        assertThat(pd.properties!!["code"]).isEqualTo(144)
    }

    @Test
    fun `not found exception returns not found with custom code`() {
        val ex = NotFoundException(999, "Missing")
        val resp = handler.handleNotFoundException(ex)

        assertThat(resp.statusCode.value()).isEqualTo(HttpStatus.NOT_FOUND.value())
        val pd = resp.body!!
        assertThat(pd.status).isEqualTo(HttpStatus.NOT_FOUND.value())
        assertThat(pd.title).isEqualTo("Not_Found error")
        assertThat(pd.detail).isEqualTo("Missing")
        assertThat(pd.properties!!["code"]).isEqualTo(999)
    }

    @Test
    fun `invalid request exception returns bad request with custom code`() {
        val ex = InvalidRequestException(321, "Bad request")
        val resp = handler.handleInvalidRequestException(ex)

        assertThat(resp.statusCode.value()).isEqualTo(HttpStatus.BAD_REQUEST.value())
        val pd = resp.body!!
        assertThat(pd.status).isEqualTo(HttpStatus.BAD_REQUEST.value())
        assertThat(pd.title).isEqualTo("Bad Request error")
        assertThat(pd.detail).isEqualTo("Bad request")
        assertThat(pd.properties!!["code"]).isEqualTo(321)
    }

    @Test
    fun `unauthorized request exception returns unauthorized with custom code`() {
        val ex = UnauthorizedRequestException(12, "No auth")
        val resp = handler.handleUnauthorizedRequestException(ex)

        assertThat(resp.statusCode.value()).isEqualTo(HttpStatus.UNAUTHORIZED.value())
        val pd = resp.body!!
        assertThat(pd.status).isEqualTo(HttpStatus.UNAUTHORIZED.value())
        assertThat(pd.title).isEqualTo("Unauthorized error")
        assertThat(pd.detail).isEqualTo("No auth")
        assertThat(pd.properties!!["code"]).isEqualTo(12)
    }

    @Test
    fun `timeout exception returns unauthorized with code from exception`() {
        val ex = TimeOutException(55, "Timed out")
        val resp = handler.handleTimeOutException(ex)

        assertThat(resp.statusCode.value()).isEqualTo(HttpStatus.UNAUTHORIZED.value())
        val pd = resp.body!!
        assertThat(pd.status).isEqualTo(HttpStatus.UNAUTHORIZED.value())
        assertThat(pd.title).isEqualTo("Unauthorized error")
        assertThat(pd.detail).isEqualTo("Timed out")
        assertThat(pd.properties!!["code"]).isEqualTo(55)
    }

    @Test
    fun `date time exception returns bad request`() {
        val ex = DateTimeException("bad date")
        val resp = handler.handleInvalidMonth(ex)

        assertThat(resp.statusCode.value()).isEqualTo(HttpStatus.BAD_REQUEST.value())
        val pd = resp.body!!
        assertThat(pd.status).isEqualTo(HttpStatus.BAD_REQUEST.value())
        assertThat(pd.title).isEqualTo("Date time error")
        assertThat(pd.detail).isEqualTo("bad date")
        assertThat(pd.properties!!["code"]).isEqualTo(68)
    }

    @Test
    fun `type mismatch exception returns bad request with code 67`() {
        val ex = Mockito.mock(TypeMismatchException::class.java)
        Mockito.`when`(ex.message).thenReturn("Type mismatch")
        val resp = handler.handleTypeMismatch(ex)

        assertThat(resp.statusCode.value()).isEqualTo(HttpStatus.BAD_REQUEST.value())
        val pd = resp.body!!
        assertThat(pd.status).isEqualTo(HttpStatus.BAD_REQUEST.value())
        assertThat(pd.title).isEqualTo("Type Mismatch error")
        assertThat(pd.detail).isEqualTo("Type mismatch")
        assertThat(pd.properties!!["code"]).isEqualTo(67)
    }

    @Test
    fun `internal server error exception returns 500 with code 111`() {
        val ex = InternalServerErrorException(13, "internal")
        val resp = handler.onIrregularException(ex)

        assertThat(resp.statusCode.value()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR.value())
        val pd = resp.body!!
        assertThat(pd.status).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR.value())
        assertThat(pd.title).isEqualTo("Internal server error")
        assertThat(pd.detail).contains("Oops, something went wrong")
        assertThat(pd.properties!!["code"]).isEqualTo(111)
    }

    @Test
    fun `illegal argument with invalid uuid returns not found`() {
        val ex = IllegalArgumentException("Invalid UUID string: 1234")
        val resp = handler.onIllegalArgumentException(ex)

        assertThat(resp.statusCode.value()).isEqualTo(HttpStatus.NOT_FOUND.value())
        val pd = resp.body!!
        assertThat(pd.status).isEqualTo(HttpStatus.NOT_FOUND.value())
        assertThat(pd.title).isEqualTo("Resource not found")
        assertThat(pd.detail).isEqualTo("The requested resource does not exist")
        assertThat(pd.properties!!["code"]).isEqualTo(404)
    }

    @Test
    fun `illegal argument generic returns bad request`() {
        val ex = IllegalArgumentException("bad value")
        val resp = handler.onIllegalArgumentException(ex)

        assertThat(resp.statusCode.value()).isEqualTo(HttpStatus.BAD_REQUEST.value())
        val pd = resp.body!!
        assertThat(pd.status).isEqualTo(HttpStatus.BAD_REQUEST.value())
        assertThat(pd.title).isEqualTo("Bad Request")
        assertThat(pd.detail).contains("Invalid argument")
        assertThat(pd.properties!!["code"]).isEqualTo(65)
    }

    @Test
    fun `illegal argument without message still returns bad request`() {
        val ex = IllegalArgumentException()
        val resp = handler.onIllegalArgumentException(ex)

        assertThat(resp.statusCode.value()).isEqualTo(HttpStatus.BAD_REQUEST.value())
        val pd = resp.body!!
        assertThat(pd.status).isEqualTo(HttpStatus.BAD_REQUEST.value())
        assertThat(pd.title).isEqualTo("Bad Request")
        assertThat(pd.properties!!["code"]).isEqualTo(65)
    }

    @Test
    fun `forbidden exception returns forbidden with custom code`() {
        val ex = ForbiddenException(777, "Access denied")
        val resp = handler.handleForbiddenException(ex)

        assertThat(resp.statusCode.value()).isEqualTo(HttpStatus.FORBIDDEN.value())
        val pd = resp.body!!
        assertThat(pd.status).isEqualTo(HttpStatus.FORBIDDEN.value())
        assertThat(pd.title).isEqualTo("Forbidden error")
        assertThat(pd.detail).isEqualTo("Access denied")
        assertThat(pd.properties!!["code"]).isEqualTo(777)
    }

    @Test
    fun `generic exception returns internal server error with code 111`() {
        val ex = Exception("boom")
        val resp = handler.onIrregularException(ex)

        assertThat(resp.statusCode.value()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR.value())
        val pd = resp.body!!
        assertThat(pd.status).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR.value())
        assertThat(pd.title).isEqualTo("Internal server error")
        assertThat(pd.detail).contains("Oops, something went wrong")
        assertThat(pd.properties!!["code"]).isEqualTo(111)
    }
}
