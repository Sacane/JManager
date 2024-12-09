package fr.sacane.jmanager.infrastructure.api

import fr.sacane.jmanager.domain.models.InvalidCurrencyException
import org.springframework.http.HttpStatus
import org.springframework.http.ProblemDetail
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestController

@ControllerAdvice(annotations = [RestController::class])
class ProblemDetailHandler {
    @ExceptionHandler(Exception::class, InternalServerErrorException::class)
    fun onIrregularException(ex: Exception): ResponseEntity<ProblemDetail> {
        val problemDetail = ProblemDetail.forStatus(HttpStatus.INTERNAL_SERVER_ERROR)
        problemDetail.title = "Internal server error"
        problemDetail.detail = "Oops, something went wrong. It's our problem : ${ex.message}"
        problemDetail.setProperty("code", 111)
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(problemDetail)
    }

    @ExceptionHandler(InvalidCurrencyException::class)
    fun onIrregularException(ex: InvalidCurrencyException): ResponseEntity<ProblemDetail> {
        val problemDetail = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST)
        problemDetail.title = "Invalid given currency"
        problemDetail.detail = "Oops, something went wrong. It's our problem : ${ex.message}"
        problemDetail.setProperty("code", 144)
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
}