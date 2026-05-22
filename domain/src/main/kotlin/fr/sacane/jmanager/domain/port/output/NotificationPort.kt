package fr.sacane.jmanager.domain.port.output

import fr.sacane.jmanager.domain.hexadoc.Port
import fr.sacane.jmanager.domain.hexadoc.Side

@Port(Side.INFRASTRUCTURE)
interface NotificationPort {
    fun sendWelcomeEmail(username: String, email: String)
}
