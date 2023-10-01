package fr.sacane.jmanager.infrastructure.rest

data class ErrorMessage(
    var status: Int,
    var message: String
)

class ForbiddenException(override val message: String) : RuntimeException(message)
class TimeOutException(override val message: String) : RuntimeException(message)
class NotFoundException(override val message: String) : RuntimeException(message)
class InvalidRequestException(override val message: String) : RuntimeException(message)
class UnauthorizedRequestException(override val message: String): RuntimeException(message)