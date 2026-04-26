package fr.sacane.jmanager.domain.port.input.tag

import fr.sacane.jmanager.domain.models.SessionToken
import java.util.UUID

data class DeleteTagCommand(
    val token: SessionToken,
    val tagId: UUID,
    val force: Boolean = false
)
