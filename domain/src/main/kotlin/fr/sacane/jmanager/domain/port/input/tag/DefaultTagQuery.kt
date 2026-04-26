package fr.sacane.jmanager.domain.port.input.tag

import fr.sacane.jmanager.domain.models.SessionToken

data class DefaultTagQuery(
    val token: SessionToken
)
