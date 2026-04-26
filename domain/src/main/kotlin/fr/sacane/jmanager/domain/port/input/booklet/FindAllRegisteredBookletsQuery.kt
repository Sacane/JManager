package fr.sacane.jmanager.domain.port.input.booklet

import fr.sacane.jmanager.domain.models.SessionToken

data class FindAllRegisteredBookletsQuery(
    val token: SessionToken
)
