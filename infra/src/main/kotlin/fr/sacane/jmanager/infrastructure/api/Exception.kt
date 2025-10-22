package fr.sacane.jmanager.infrastructure.api

class ForbiddenException(val errCode: Int, override val message: String) : RuntimeException(message)
class TimeOutException(val errCode: Int, override val message: String) : RuntimeException(message)
class NotFoundException(val errCode: Int, override val message: String) : RuntimeException(message)
class InvalidRequestException(val errCode: Int, override val message: String) : RuntimeException(message)
class UnauthorizedRequestException(val errCode: Int, override val message: String): RuntimeException(message)
class InternalServerErrorException(val errCode: Int, override val message: String) : RuntimeException(message)