package fr.sacane.jmanager.infrastructure.api

class ForbiddenException(val errCode: Int, override val message: String, val errKey: String? = null) : RuntimeException(message)
class TimeOutException(val errCode: Int, override val message: String, val errKey: String? = null) : RuntimeException(message)
class NotFoundException(val errCode: Int, override val message: String, val errKey: String? = null) : RuntimeException(message)
class InvalidRequestException(val errCode: Int, override val message: String, val errKey: String? = null) : RuntimeException(message)
class UnauthorizedRequestException(val errCode: Int, override val message: String, val errKey: String? = null): RuntimeException(message)
class InternalServerErrorException(val errCode: Int, override val message: String, val errKey: String? = null) : RuntimeException(message)
class ConflictException(val errCode: Int, override val message: String, val errKey: String? = null) : RuntimeException(message)