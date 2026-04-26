package fr.sacane.jmanager.domain.port.input.booklet

import fr.sacane.jmanager.domain.models.SessionToken
import java.util.UUID

data class FindBookletByIdQuery(
    val bookletId: UUID,
    val token: SessionToken
)
