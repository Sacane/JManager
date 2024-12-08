package fr.sacane.jmanager.infrastructure.api

data class ErrorMessage(
    var status: Int,
    var message: String
)

class ForbiddenException(val errCode: Int, override val message: String) : RuntimeException(message)
class TimeOutException(val errCode: Int, override val message: String) : RuntimeException(message)
class NotFoundException(val errCode: Int, override val message: String) : RuntimeException(message)
class InvalidRequestException(val errCode: Int, override val message: String) : RuntimeException(message)
class UnauthorizedRequestException(val errCode: Int, override val message: String): RuntimeException(message)
class InternalServerErrorException(val errCode: Int, override val message: String) : RuntimeException(message)