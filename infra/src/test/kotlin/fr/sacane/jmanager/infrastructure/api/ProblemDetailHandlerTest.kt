package fr.sacane.jmanager.infrastructure.api

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus
import org.springframework.web.bind.MissingServletRequestParameterException

class ProblemDetailHandlerTest {

    private val handler = ProblemDetailHandler()

    @Test
    fun `handle missing parameter returns bad request with code 65`() {
        val ex = MissingServletRequestParameterException("id", "String")
        val resp = handler.handleMissingParams(ex)

        // Compare status as integer value to avoid HttpStatus enum/string mismatch
        assertThat(resp.statusCode.value()).isEqualTo(HttpStatus.BAD_REQUEST.value())
        val pd = resp.body!!
        // ProblemDetail.status is an integer (nullable), compare to the numeric value
        assertThat(pd.status).isEqualTo(HttpStatus.BAD_REQUEST.value())
        assertThat(pd.title).isEqualTo("Bad Request")
        assertThat(pd.detail).contains("Missing mandatory parameter")
        assertThat(pd.properties["code"]).isEqualTo(65)
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
        assertThat(pd.properties["code"]).isEqualTo(404)
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
        assertThat(pd.properties["code"]).isEqualTo(65)
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
        assertThat(pd.properties["code"]).isEqualTo(777)
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
        assertThat(pd.properties["code"]).isEqualTo(111)
    }
}
