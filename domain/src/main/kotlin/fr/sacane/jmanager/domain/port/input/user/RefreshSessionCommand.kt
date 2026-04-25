package fr.sacane.jmanager.domain.port.input.user

import java.util.UUID

data class RefreshSessionCommand(val refreshToken: UUID)
