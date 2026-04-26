package fr.sacane.jmanager.domain.port.input.tag

import fr.sacane.jmanager.domain.models.SessionToken

data class GetAllTagsQuery(
    val token: SessionToken
)
