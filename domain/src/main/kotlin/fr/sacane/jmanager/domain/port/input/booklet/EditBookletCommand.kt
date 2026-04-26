package fr.sacane.jmanager.domain.port.input.booklet

import fr.sacane.jmanager.domain.models.Booklet
import fr.sacane.jmanager.domain.models.SessionToken

data class EditBookletCommand(
    val booklet: Booklet,
    val token: SessionToken
)
