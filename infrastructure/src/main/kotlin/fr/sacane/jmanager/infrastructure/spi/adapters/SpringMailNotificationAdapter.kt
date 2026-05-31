package fr.sacane.jmanager.infrastructure.spi.adapters

import fr.sacane.jmanager.domain.hexadoc.Adapter
import fr.sacane.jmanager.domain.hexadoc.Side
import fr.sacane.jmanager.domain.models.SubscriptionPlan
import fr.sacane.jmanager.domain.port.output.NotificationPort
import fr.sacane.jmanager.infrastructure.spi.adapters.mail.EmailTemplates
import org.springframework.beans.factory.annotation.Value
import org.springframework.mail.javamail.JavaMailSender
import org.springframework.mail.javamail.MimeMessageHelper
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Service
import java.util.logging.Logger

@Service
@Adapter(Side.INFRASTRUCTURE)
class SpringMailNotificationAdapter(
    private val mailSender: JavaMailSender,
    @param:Value("\${spring.mail.from:noreply@jmanager.sacane.fr}") private val fromValue: String,
    @param:Value("\${app.url:http://localhost:3000}") private val appUrl: String,
) : NotificationPort {

    companion object {
        private val LOGGER = Logger.getLogger(SpringMailNotificationAdapter::class.java.name)
    }

    @Async
    override fun sendVerificationEmail(email: String, token: String) {
        val verificationLink = "$appUrl/verify-email?token=$token"
        val content = EmailTemplates.verificationEmail(verificationLink)
        try {
            val mimeMessage = mailSender.createMimeMessage()
            MimeMessageHelper(mimeMessage, false, "UTF-8").apply {
                setTo(email)
                setFrom(fromValue)
                setSubject(content.subject)
                setText(content.htmlBody, true)
            }
            mailSender.send(mimeMessage)
        } catch (e: Exception) {
            LOGGER.severe("Failed to send verification email: ${e.javaClass.simpleName}")
        }
    }

    @Async
    override fun sendWelcomeWithVerificationEmail(
        username: String,
        email: String,
        subscriptionPlan: SubscriptionPlan,
        verificationToken: String,
    ) {
        val verificationLink = "$appUrl/verify-email?token=$verificationToken"
        val content = when (subscriptionPlan) {
            SubscriptionPlan.BETA_TESTER -> EmailTemplates.betaTester(username, verificationLink)
            SubscriptionPlan.FREE        -> EmailTemplates.freeUser(username, verificationLink)
            SubscriptionPlan.PREMIUM     -> EmailTemplates.premiumUser(username, verificationLink)
        }
        try {
            val mimeMessage = mailSender.createMimeMessage()
            MimeMessageHelper(mimeMessage, false, "UTF-8").apply {
                setTo(email)
                setFrom(fromValue)
                setSubject(content.subject)
                setText(content.htmlBody, true)
            }
            mailSender.send(mimeMessage)
        } catch (e: Exception) {
            LOGGER.severe("Failed to send welcome email: ${e.javaClass.simpleName}")
        }
    }
}
