package fr.sacane.jmanager.infrastructure.rest

data class ErrorMessage(
    var status: Int? = null,
    var message: String? = null
)

class ForbiddenException(s: String) : RuntimeException(s)
class TimeOutException(s: String) : RuntimeException(s)
class NotFoundException(s: String) : RuntimeException(s)
class InvalidRequestException(s: String) : RuntimeException(s)
class UnauthorizedRequestException(s: String): RuntimeException(s)