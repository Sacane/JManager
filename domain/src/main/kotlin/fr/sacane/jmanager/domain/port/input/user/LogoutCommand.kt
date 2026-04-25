package fr.sacane.jmanager.domain.port.input.user

import fr.sacane.jmanager.domain.models.SessionToken

data class LogoutCommand(val token: SessionToken)
