package fr.sacane.jmanager.domain.port.input.tag

import fr.sacane.jmanager.domain.models.SessionToken
import fr.sacane.jmanager.domain.models.Tag

data class AddTagCommand(
    val token: SessionToken,
    val tag: Tag
)
