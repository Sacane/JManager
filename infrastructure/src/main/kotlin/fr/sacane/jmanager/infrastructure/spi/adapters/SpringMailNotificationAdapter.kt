package fr.sacane.jmanager.infrastructure.spi.adapters

import fr.sacane.jmanager.domain.hexadoc.Adapter
import fr.sacane.jmanager.domain.hexadoc.Side
import fr.sacane.jmanager.domain.port.output.NotificationPort
import org.springframework.beans.factory.annotation.Value
import org.springframework.mail.SimpleMailMessage
import org.springframework.mail.javamail.JavaMailSender
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Service
import java.util.logging.Logger

@Service
@Adapter(Side.INFRASTRUCTURE)
class SpringMailNotificationAdapter(
    private val mailSender: JavaMailSender,
    @param:Value("\${spring.mail.from:noreply@jmanager.app}") private val fromValue: String,
) : NotificationPort {

    companion object {
        private val LOGGER = Logger.getLogger(SpringMailNotificationAdapter::class.java.name)
    }

    @Async
    override fun sendWelcomeEmail(username: String, email: String) {
        try {
            val message = SimpleMailMessage().apply {
                setTo(email)
                from = fromValue
                subject = "Bienvenue sur JManager !"
                text = "Bonjour $username,\n\nVotre compte JManager a été créé avec succès.\n\nÀ bientôt sur JManager !"
            }
            mailSender.send(message)
        } catch (e: Exception) {
            LOGGER.severe("Failed to send welcome email to $email: ${e.message}")
        }
    }
}
