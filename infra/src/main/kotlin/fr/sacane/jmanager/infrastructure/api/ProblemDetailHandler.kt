package fr.sacane.jmanager.infrastructure.api

import fr.sacane.jmanager.domain.models.InvalidCurrencyException
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ProblemDetail
import org.springframework.http.ResponseEntity
import org.springframework.web.ErrorResponse
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.MissingServletRequestParameterException
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException

@ControllerAdvice(annotations = [RestController::class])
class ProblemDetailHandler {

    companion object {
        private val LOGGER = LoggerFactory.getLogger(ProblemDetailHandler::class.java)
    }


    @ExceptionHandler(MissingServletRequestParameterException::class)
    fun handleMissingParams(ex: MissingServletRequestParameterException): ResponseEntity<ProblemDetail> {
        val problemDetail = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST)
        problemDetail.title = "Bad Request"
        problemDetail.detail = "Missing mandatory parameter : ${ex.message}"
        problemDetail.setProperty("code", 65)
        LOGGER.error("Bad Request : {}", ex.cause?.message ?: ex.message)
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(problemDetail)
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException::class)
    fun handleTypeMismatch(ex: MethodArgumentTypeMismatchException): ResponseEntity<ProblemDetail> {
        val problemDetail = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST)
        problemDetail.title = "Bad Request"
        problemDetail.detail = "Invalid type parameter : ${ex.message}"
        problemDetail.setProperty("code", 65)
        LOGGER.error("Bad Request : {}", ex.cause?.message ?: ex.message)
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(problemDetail)
    }

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleValidationErrors(ex: MethodArgumentNotValidException): ResponseEntity<ProblemDetail> {
        val errors = ex.bindingResult.fieldErrors.associate {
            it.field to (it.defaultMessage ?: "Valeur invalide")
        }

        val problemDetail = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST)
        problemDetail.title = "Bad Request"
        problemDetail.detail = "invalid value : ${ex.message}"
        problemDetail.setProperty("code", 65)
        LOGGER.error("Bad Request : {}", ex.cause?.message ?: ex.message)
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(problemDetail)
    }

    @ExceptionHandler(Exception::class, InternalServerErrorException::class)
    fun onIrregularException(ex: Exception): ResponseEntity<ProblemDetail> {
        val problemDetail = ProblemDetail.forStatus(HttpStatus.INTERNAL_SERVER_ERROR)
        problemDetail.title = "Internal server error"
        problemDetail.detail = "Oops, something went wrong. It's our problem : ${ex.message}"
        problemDetail.setProperty("code", 111)
        LOGGER.error("Internal server error : {}", ex.cause?.message ?: ex.message)
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(problemDetail)
    }

    @ExceptionHandler(InvalidCurrencyException::class)
    fun onCurrencyException(ex: InvalidCurrencyException): ResponseEntity<ProblemDetail> {
        val problemDetail = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST)
        problemDetail.title = "Invalid given currency"
        problemDetail.detail = "Oops, something went wrong. It's our problem : ${ex.message}"
        problemDetail.setProperty("code", 144)
        LOGGER.error("InvalidCurrencyException : {}", ex.message)
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(problemDetail)
    }


    @ExceptionHandler(ForbiddenException::class)
    fun handleForbiddenException(ex: ForbiddenException): ResponseEntity<ProblemDetail> {
        val problemDetail = ProblemDetail.forStatus(HttpStatus.FORBIDDEN)
        problemDetail.title = "Forbidden error"
        problemDetail.detail = ex.message
        problemDetail.setProperty("code", ex.errCode)
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(problemDetail)
    }

    @ExceptionHandler(NotFoundException::class)
    fun handleForbiddenException(ex: NotFoundException): ResponseEntity<ProblemDetail> {
        val problemDetail = ProblemDetail.forStatus(HttpStatus.NOT_FOUND)
        problemDetail.title = "Not_Found error"
        problemDetail.detail = ex.message
        problemDetail.setProperty("code", ex.errCode)
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(problemDetail)
    }

    @ExceptionHandler(InvalidRequestException::class)
    fun handleForbiddenException(ex: InvalidRequestException): ResponseEntity<ProblemDetail> {
        val problemDetail = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST)
        problemDetail.title = "Bad Request error"
        problemDetail.detail = ex.message
        problemDetail.setProperty("code", ex.errCode)
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(problemDetail)
    }

    @ExceptionHandler(UnauthorizedRequestException::class)
    fun handleForbiddenException(ex: UnauthorizedRequestException): ResponseEntity<ProblemDetail> {
        val problemDetail = ProblemDetail.forStatus(HttpStatus.UNAUTHORIZED)
        problemDetail.title = "Unauthorized error"
        problemDetail.detail = ex.message
        problemDetail.setProperty("code", ex.errCode)
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(problemDetail)
    }
    @ExceptionHandler(TimeOutException::class)
    fun handleForbiddenException(ex: TimeOutException): ResponseEntity<ProblemDetail> {
        val problemDetail = ProblemDetail.forStatus(HttpStatus.UNAUTHORIZED)
        problemDetail.title = "Unauthorized error"
        problemDetail.detail = ex.message
        problemDetail.setProperty("code", ex.errCode)
        LOGGER.error("TimeOutException : {} with code {}", ex.message, ex.errCode)
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(problemDetail)
    }
}