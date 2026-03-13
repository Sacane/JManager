package fr.sacane.jmanager.infrastructure.api

import fr.sacane.jmanager.domain.models.InvalidCurrencyException
import org.slf4j.LoggerFactory
import org.springframework.beans.TypeMismatchException
import org.springframework.http.HttpStatus
import org.springframework.http.ProblemDetail
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.MissingServletRequestParameterException
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException
import java.time.DateTimeException

@ControllerAdvice(annotations = [RestController::class])
class ProblemDetailHandler {

    companion object {
        private val LOGGER = LoggerFactory.getLogger(ProblemDetailHandler::class.java)
    }

    private fun buildResponse(
        status: HttpStatus,
        title: String,
        detail: String?,
        code: Int
    ): ResponseEntity<ProblemDetail> {
        val problemDetail = ProblemDetail.forStatus(status)
        problemDetail.title = title
        problemDetail.detail = detail
        problemDetail.setProperty("code", code)
        return ResponseEntity.status(status).body(problemDetail)
    }

    private fun logException(context: String, message: String?) {
        LOGGER.error("{} : {}", context, message)
    }


    @ExceptionHandler(MissingServletRequestParameterException::class)
    fun handleMissingParams(ex: MissingServletRequestParameterException): ResponseEntity<ProblemDetail> {
        logException("Bad Request", ex.cause?.message ?: ex.message)
        return buildResponse(
            status = HttpStatus.BAD_REQUEST,
            title = "Bad Request",
            detail = "Missing mandatory parameter : ${ex.message}",
            code = 65
        )
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException::class)
    fun handleTypeMismatch(ex: MethodArgumentTypeMismatchException): ResponseEntity<ProblemDetail> {
        logException("Method type mismatch", ex.cause?.message ?: ex.message)
        return buildResponse(
            status = HttpStatus.BAD_REQUEST,
            title = "Method type mismatch",
            detail = "Invalid type parameter : ${ex.message}",
            code = 65
        )
    }

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleValidationErrors(ex: MethodArgumentNotValidException): ResponseEntity<ProblemDetail> {
        logException("Method argument not valid", ex.cause?.message ?: ex.message)
        return buildResponse(
            status = HttpStatus.BAD_REQUEST,
            title = "Method argument not valid",
            detail = "invalid value : ${ex.message}",
            code = 65
        )
    }

    @ExceptionHandler(Exception::class, InternalServerErrorException::class)
    fun onIrregularException(ex: Exception): ResponseEntity<ProblemDetail> {
        logException("Internal server error", ex.cause?.message ?: ex.message)
        return buildResponse(
            status = HttpStatus.INTERNAL_SERVER_ERROR,
            title = "Internal server error",
            detail = "Oops, something went wrong. It's our problem : ${ex.message}",
            code = 111
        )
    }

    @ExceptionHandler(InvalidCurrencyException::class)
    fun onCurrencyException(ex: InvalidCurrencyException): ResponseEntity<ProblemDetail> {
        logException("InvalidCurrencyException", ex.message)
        return buildResponse(
            status = HttpStatus.BAD_REQUEST,
            title = "Invalid given currency",
            detail = "Oops, something went wrong. It's our problem : ${ex.message}",
            code = 144
        )
    }

    @ExceptionHandler(IllegalArgumentException::class)
    fun onIllegalArgumentException(ex: IllegalArgumentException): ResponseEntity<ProblemDetail> {
        if (ex.message?.contains("Invalid UUID string") == true) {
            logException("Invalid UUID provided", ex.message)
            return buildResponse(
                status = HttpStatus.NOT_FOUND,
                title = "Resource not found",
                detail = "The requested resource does not exist",
                code = 404
            )
        }
        logException("IllegalArgumentException", ex.message)
        return buildResponse(
            status = HttpStatus.BAD_REQUEST,
            title = "Bad Request",
            detail = "Invalid argument : ${ex.message}",
            code = 65
        )
    }


    @ExceptionHandler(ForbiddenException::class)
    fun handleForbiddenException(ex: ForbiddenException): ResponseEntity<ProblemDetail> {
        return buildResponse(
            status = HttpStatus.FORBIDDEN,
            title = "Forbidden error",
            detail = ex.message,
            code = ex.errCode
        )
    }

    @ExceptionHandler(NotFoundException::class)
    fun handleNotFoundException(ex: NotFoundException): ResponseEntity<ProblemDetail> {
        return buildResponse(
            status = HttpStatus.NOT_FOUND,
            title = "Not_Found error",
            detail = ex.message,
            code = ex.errCode
        )
    }

    @ExceptionHandler(InvalidRequestException::class)
    fun handleInvalidRequestException(ex: InvalidRequestException): ResponseEntity<ProblemDetail> {
        return buildResponse(
            status = HttpStatus.BAD_REQUEST,
            title = "Bad Request error",
            detail = ex.message,
            code = ex.errCode
        )
    }

    @ExceptionHandler(UnauthorizedRequestException::class)
    fun handleUnauthorizedRequestException(ex: UnauthorizedRequestException): ResponseEntity<ProblemDetail> {
        return buildResponse(
            status = HttpStatus.UNAUTHORIZED,
            title = "Unauthorized error",
            detail = ex.message,
            code = ex.errCode
        )
    }

    @ExceptionHandler(TimeOutException::class)
    fun handleTimeOutException(ex: TimeOutException): ResponseEntity<ProblemDetail> {
        LOGGER.error("TimeOutException : {} with code {}", ex.message, ex.errCode)
        return buildResponse(
            status = HttpStatus.UNAUTHORIZED,
            title = "Unauthorized error",
            detail = ex.message,
            code = ex.errCode
        )
    }

    @ExceptionHandler(DateTimeException::class)
    fun handleInvalidMonth(ex: DateTimeException): ResponseEntity<ProblemDetail> {
        logException("DateTimeException", ex.message)
        return buildResponse(
            status = HttpStatus.BAD_REQUEST,
            title = "Date time error",
            detail = ex.message,
            code = 68
        )
    }

    @ExceptionHandler(TypeMismatchException::class)
    fun handleTypeMismatch(ex: TypeMismatchException): ResponseEntity<ProblemDetail> {
        logException("TypeMismatchException", ex.message)
        return buildResponse(
            status = HttpStatus.BAD_REQUEST,
            title = "Type Mismatch error",
            detail = ex.message,
            code = 67
        )
    }
}