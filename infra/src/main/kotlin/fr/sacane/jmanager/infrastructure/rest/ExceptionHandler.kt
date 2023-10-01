package fr.sacane.jmanager.infrastructure.rest

import org.springframework.http.HttpStatus
import org.springframework.http.HttpStatusCode
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestController

@ControllerAdvice(annotations = [RestController::class])
class ExceptionHandler {

    @ExceptionHandler(ForbiddenException::class)
    fun handleForbiddenException(ex: ForbiddenException): ResponseEntity<ErrorMessage> {
        val errorMessage = ErrorMessage(
            HttpStatus.FORBIDDEN.value(),
            ex.message
        )
        return ResponseEntity(errorMessage, HttpStatus.FORBIDDEN)
    }

    @ExceptionHandler(InvalidRequestException::class)
    fun handleTimeoutException(ex: InvalidRequestException): ResponseEntity<ErrorMessage> {
        val errorMessage = ErrorMessage(
            HttpStatus.BAD_REQUEST.value(),
            ex.message
        )
        return ResponseEntity(errorMessage, HttpStatus.BAD_REQUEST)
    }
    @ExceptionHandler(TimeOutException::class)
    fun handleUnauthorizedException(ex: TimeOutException): ResponseEntity<ErrorMessage> {
        val errorMessage = ErrorMessage(
            HttpStatus.TEMPORARY_REDIRECT.value(),
            ex.message
        )
        return ResponseEntity(errorMessage, HttpStatus.TEMPORARY_REDIRECT)
    }
    @ExceptionHandler(NotFoundException::class)
    fun handleNotFoundException(ex: NotFoundException): ResponseEntity<ErrorMessage> {
        val errorMessage = ErrorMessage(
            HttpStatus.NOT_FOUND.value(),
            ex.message
        )
        return ResponseEntity(errorMessage, HttpStatus.NOT_FOUND)
    }

    @ExceptionHandler(UnauthorizedRequestException::class)
    fun handleUnauthorizedRequestException(ex: UnauthorizedRequestException): ResponseEntity<ErrorMessage>{
        val errorMessage = ErrorMessage(
            HttpStatus.UNAUTHORIZED.value(),
            ex.message
        )
        return ResponseEntity(errorMessage, HttpStatus.UNAUTHORIZED)
    }
}