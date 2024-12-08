package fr.sacane.jmanager.infrastructure.api

import org.springframework.http.HttpStatus
import org.springframework.http.ProblemDetail
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestController

@ControllerAdvice(annotations = [RestController::class])
class ProblemDetailHandler {
    @ExceptionHandler(Exception::class, InternalServerErrorException::class)
    fun handleForbiddenException(ex: Exception): ResponseEntity<ProblemDetail> {
        val problemDetail = ProblemDetail.forStatus(HttpStatus.FORBIDDEN)
        problemDetail.title = "Internal server error"
        problemDetail.detail = "Oops, something went wrong. It's our problem : ${ex.message}"
        problemDetail.setProperty("code", 111)
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(problemDetail)
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
        val problemDetail = ProblemDetail.forStatus(HttpStatus.FORBIDDEN)
        problemDetail.title = "NotFound error"
        problemDetail.detail = ex.message
        problemDetail.setProperty("code", ex.errCode)
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(problemDetail)
    }

    @ExceptionHandler(InvalidRequestException::class)
    fun handleForbiddenException(ex: InvalidRequestException): ResponseEntity<ProblemDetail> {
        val problemDetail = ProblemDetail.forStatus(HttpStatus.FORBIDDEN)
        problemDetail.title = "Forbidden error"
        problemDetail.detail = ex.message
        problemDetail.setProperty("code", ex.errCode)
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(problemDetail)
    }

    @ExceptionHandler(UnauthorizedRequestException::class)
    fun handleForbiddenException(ex: UnauthorizedRequestException): ResponseEntity<ProblemDetail> {
        val problemDetail = ProblemDetail.forStatus(HttpStatus.FORBIDDEN)
        problemDetail.title = "Forbidden error"
        problemDetail.detail = ex.message
        problemDetail.setProperty("code", ex.errCode)
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(problemDetail)
    }
}