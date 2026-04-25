package fr.sacane.jmanager.domain.port.input.user

import fr.sacane.jmanager.domain.models.BookletMonthlyCycleUpdate
import fr.sacane.jmanager.domain.models.SessionToken
import java.util.UUID

data class UpdateUserSettingsCommand(
    val token: SessionToken,
    val projectionWindowDays: Int,
    val bookletCycles: Map<UUID, BookletMonthlyCycleUpdate>
)
