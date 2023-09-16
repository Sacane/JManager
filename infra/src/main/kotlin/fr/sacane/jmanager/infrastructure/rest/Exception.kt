package fr.sacane.jmanager.infrastructure.rest

import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.ResponseStatus



@ResponseStatus(value= HttpStatus.FORBIDDEN)
class ForbiddenException: RuntimeException {
    constructor(s: String): super(s)
    constructor(s: String, t: Throwable): super(s, t)
}

@ResponseStatus(value= HttpStatus.NOT_FOUND)
class NotFoundException: RuntimeException {
    constructor(s: String): super(s)
    constructor(s: String, t: Throwable): super(s, t)
}

@ResponseStatus(value= HttpStatus.BAD_REQUEST)
class InvalidRequestException: RuntimeException {
    constructor(s: String): super(s)
    constructor(s: String, t: Throwable): super(s, t)
}