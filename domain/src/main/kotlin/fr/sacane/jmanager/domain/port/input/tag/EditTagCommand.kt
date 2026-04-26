package fr.sacane.jmanager.domain.port.input.tag

import fr.sacane.jmanager.domain.models.SessionToken
import fr.sacane.jmanager.domain.models.Tag

data class EditTagCommand(
    val token: SessionToken,
    val tag: Tag
)
