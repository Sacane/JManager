package fr.sacane.jmanager.application.api

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.slf4j.MDC
import org.springframework.http.HttpStatus
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.bind.MissingServletRequestParameterException
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.beans.TypeMismatchException
import java.time.DateTimeException
import java.util.UUID
import org.mockito.Mockito
import fr.sacane.jmanager.domain.models.InvalidCurrencyException
import fr.sacane.jmanager.domain.models.Role
import fr.sacane.jmanager.domain.port.input.MdcKeys
import fr.sacane.jmanager.domain.utils.ErrorCatalog
import fr.sacane.jmanager.domain.utils.ResultState

class ProblemDetailHandlerTest {

    private val handler = ProblemDetailHandler()

    @AfterEach
    fun cleanup() {
        MDC.remove(MdcKeys.REQUEST_ID)
        SecurityContextHolder.clearContext()
    }

    @Test
    fun `handle missing parameter returns bad request with code 65`() {
        val ex = MissingServletRequestParameterException("id", "String")
        val resp = handler.handleMissingParams(ex)

        assertThat(resp.statusCode.value()).isEqualTo(HttpStatus.BAD_REQUEST.value())
        val pd = resp.body!!
        assertThat(pd.status).isEqualTo(HttpStatus.BAD_REQUEST.value())
        assertThat(pd.title).isEqualTo("Bad Request")
        assertThat(pd.detail).contains("Missing mandatory parameter")
        assertThat(pd.properties!!["code"]).isEqualTo(ErrorCatalog.REQUEST_VALIDATION)
        assertThat(pd.properties!!["errorKey"]).isEqualTo(ErrorCatalog.keyForCode(ErrorCatalog.REQUEST_VALIDATION))
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
        assertThat(pd.detail).contains("Invalid type for the provided parameter")
        assertThat(pd.properties!!["code"]).isEqualTo(ErrorCatalog.REQUEST_VALIDATION)
        assertThat(pd.properties!!["errorKey"]).isEqualTo(ErrorCatalog.keyForCode(ErrorCatalog.REQUEST_VALIDATION))
    }

    @Test
    fun `method argument not valid returns bad request`() {
        val ex = Mockito.mock(MethodArgumentNotValidException::class.java)
        val bindingResult = Mockito.mock(org.springframework.validation.BindingResult::class.java)
        Mockito.`when`(ex.message).thenReturn("Validation failed")
        Mockito.`when`(ex.bindingResult).thenReturn(bindingResult)
        Mockito.`when`(bindingResult.fieldErrors).thenReturn(emptyList())
        val resp = handler.handleValidationErrors(ex)

        assertThat(resp.statusCode.value()).isEqualTo(HttpStatus.BAD_REQUEST.value())
        val pd = resp.body!!
        assertThat(pd.status).isEqualTo(HttpStatus.BAD_REQUEST.value())
        assertThat(pd.title).isEqualTo("Method argument not valid")
        assertThat(pd.detail).isEqualTo("Invalid request body")
        assertThat(pd.properties!!["code"]).isEqualTo(ErrorCatalog.REQUEST_VALIDATION)
        assertThat(pd.properties!!["errorKey"]).isEqualTo(ErrorCatalog.keyForCode(ErrorCatalog.REQUEST_VALIDATION))
    }

    @Test
    fun `invalid currency exception returns bad request with code 144`() {
        val ex = InvalidCurrencyException("nope")
        val resp = handler.onCurrencyException(ex)

        assertThat(resp.statusCode.value()).isEqualTo(HttpStatus.BAD_REQUEST.value())
        val pd = resp.body!!
        assertThat(pd.status).isEqualTo(HttpStatus.BAD_REQUEST.value())
        assertThat(pd.title).isEqualTo("Invalid given currency")
        assertThat(pd.detail).contains("The provided currency is not supported")
        assertThat(pd.properties!!["errorKey"]).isEqualTo(ErrorCatalog.keyForCode(ErrorCatalog.INVALID_CURRENCY))
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
        assertThat(pd.properties!!["errorKey"]).isEqualTo("unknown.error")
    }

    @Test
    fun `not found exception preserves explicit error key`() {
        val ex = NotFoundException(999, "Missing", "domain.booklet.not_found")
        val resp = handler.handleNotFoundException(ex)

        assertThat(resp.statusCode.value()).isEqualTo(HttpStatus.NOT_FOUND.value())
        val pd = resp.body!!
        assertThat(pd.properties!!["code"]).isEqualTo(999)
        assertThat(pd.properties!!["errorKey"]).isEqualTo("domain.booklet.not_found")
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
        assertThat(pd.properties!!["errorKey"]).isEqualTo("unknown.error")
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
        assertThat(pd.properties!!["errorKey"]).isEqualTo("unknown.error")
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
        assertThat(pd.properties!!["errorKey"]).isEqualTo("unknown.error")
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
        assertThat(pd.properties!!["code"]).isEqualTo(ErrorCatalog.REQUEST_DATE_TIME_INVALID)
        assertThat(pd.properties!!["errorKey"]).isEqualTo(ErrorCatalog.keyForCode(ErrorCatalog.REQUEST_DATE_TIME_INVALID))
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
        assertThat(pd.properties!!["code"]).isEqualTo(ErrorCatalog.REQUEST_TYPE_MISMATCH)
        assertThat(pd.properties!!["errorKey"]).isEqualTo(ErrorCatalog.keyForCode(ErrorCatalog.REQUEST_TYPE_MISMATCH))
    }

    @Test
    fun `internal server error exception returns 500 with propagated code`() {
        val ex = InternalServerErrorException(ResultState.INTERNAL_SERVER_ERROR.code, "internal")
        val resp = handler.onInternalServerErrorException(ex)

        assertThat(resp.statusCode.value()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR.value())
        val pd = resp.body!!
        assertThat(pd.status).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR.value())
        assertThat(pd.title).isEqualTo("Internal server error")
        assertThat(pd.detail).contains("An unexpected internal error occurred")
        assertThat(pd.properties!!["code"]).isEqualTo(ResultState.INTERNAL_SERVER_ERROR.code)
        assertThat(pd.properties!!["errorKey"]).isEqualTo(ErrorCatalog.keyForCode(ResultState.INTERNAL_SERVER_ERROR.code))
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
        assertThat(pd.properties!!["code"]).isEqualTo(ErrorCatalog.INVALID_UUID_NOT_FOUND)
        assertThat(pd.properties!!["errorKey"]).isEqualTo(ErrorCatalog.keyForCode(ErrorCatalog.INVALID_UUID_NOT_FOUND))
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
        assertThat(pd.properties!!["code"]).isEqualTo(ErrorCatalog.REQUEST_VALIDATION)
        assertThat(pd.properties!!["errorKey"]).isEqualTo(ErrorCatalog.keyForCode(ErrorCatalog.REQUEST_VALIDATION))
    }

    @Test
    fun `illegal argument without message still returns bad request`() {
        val ex = IllegalArgumentException()
        val resp = handler.onIllegalArgumentException(ex)

        assertThat(resp.statusCode.value()).isEqualTo(HttpStatus.BAD_REQUEST.value())
        val pd = resp.body!!
        assertThat(pd.status).isEqualTo(HttpStatus.BAD_REQUEST.value())
        assertThat(pd.title).isEqualTo("Bad Request")
        assertThat(pd.properties!!["code"]).isEqualTo(ErrorCatalog.REQUEST_VALIDATION)
        assertThat(pd.properties!!["errorKey"]).isEqualTo(ErrorCatalog.keyForCode(ErrorCatalog.REQUEST_VALIDATION))
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
        assertThat(pd.properties!!["errorKey"]).isEqualTo("unknown.error")
    }

    @Test
    fun `requestId from MDC is included in error response`() {
        val id = UUID.randomUUID().toString()
        MDC.put(MdcKeys.REQUEST_ID, id)

        val resp = handler.handleNotFoundException(NotFoundException(404, "not found"))

        assertThat(resp.body!!.properties!!["requestId"]).isEqualTo(id)
    }

    @Test
    fun `requestId is absent when MDC has no entry`() {
        val resp = handler.handleNotFoundException(NotFoundException(404, "not found"))

        assertThat(resp.body!!.properties!!).doesNotContainKey("requestId")
    }

    @Test
    fun `userId from SecurityContext is included when authenticated`() {
        val userId = UUID.randomUUID()
        val principal = JmanagerUserAuthDetail(id = userId, username = "alice", role = setOf(Role.USER), token = "tok")
        val auth = UsernamePasswordAuthenticationToken(principal, null, emptyList())
        SecurityContextHolder.getContext().authentication = auth

        val resp = handler.handleNotFoundException(NotFoundException(404, "not found"))

        assertThat(resp.body!!.properties!!["userId"]).isEqualTo(userId.toString())
    }

    @Test
    fun `userId is absent when unauthenticated`() {
        val resp = handler.handleNotFoundException(NotFoundException(404, "not found"))

        assertThat(resp.body!!.properties!!).doesNotContainKey("userId")
    }

    @Test
    fun `generic exception returns internal server error with code 111`() {
        val ex = Exception("boom")
        val resp = handler.onIrregularException(ex)

        assertThat(resp.statusCode.value()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR.value())
        val pd = resp.body!!
        assertThat(pd.status).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR.value())
        assertThat(pd.title).isEqualTo("Internal server error")
        assertThat(pd.detail).contains("An unexpected internal error occurred")
        assertThat(pd.properties!!["code"]).isEqualTo(ErrorCatalog.INTERNAL_UNEXPECTED)
        assertThat(pd.properties!!["errorKey"]).isEqualTo(ErrorCatalog.keyForCode(ErrorCatalog.INTERNAL_UNEXPECTED))
    }
}
