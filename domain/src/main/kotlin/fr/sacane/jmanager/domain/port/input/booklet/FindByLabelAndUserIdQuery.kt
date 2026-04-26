package fr.sacane.jmanager.domain.port.input.booklet

import fr.sacane.jmanager.domain.models.SessionToken

data class FindByLabelAndUserIdQuery(
    val token: SessionToken,
    val label: String
)
