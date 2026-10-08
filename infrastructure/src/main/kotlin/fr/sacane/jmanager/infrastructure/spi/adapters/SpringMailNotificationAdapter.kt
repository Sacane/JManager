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
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale
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
        private val READER_ZONE: ZoneId = ZoneId.of("Europe/Paris")
        private val CHANGE_TIME_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("d MMMM yyyy 'à' HH:mm", Locale.FRENCH)
    }

    @Async
    override fun sendVerificationEmail(email: String, token: String) {
        val verificationLink = "$appUrl/verify-email?token=$token"
        send(email, EmailTemplates.verificationEmail(verificationLink), "verification")
    }

    // The token travels in the fragment: browsers never send it to a server, so no log or Referer can leak it.
    @Async
    override fun sendPasswordResetEmail(email: String, rawToken: String) {
        send(email, EmailTemplates.passwordReset("$appUrl/reset-password#token=$rawToken"), "password reset")
    }

    @Async
    override fun sendPasswordChangedEmail(email: String, changedAt: LocalDateTime) {
        val readerTime = changedAt.atZone(ZoneOffset.UTC).withZoneSameInstant(READER_ZONE).format(CHANGE_TIME_FORMAT)
        send(email, EmailTemplates.passwordChanged(readerTime, "$appUrl/forgot-password"), "password changed")
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
        send(email, content, "welcome")
    }

    // An SMTP failure is logged, never thrown: the caller's answer must not depend on the mail server.
    private fun send(email: String, content: EmailTemplates.EmailContent, kind: String) {
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
            LOGGER.severe("Failed to send $kind email: ${e.javaClass.simpleName}")
        }
    }
}
