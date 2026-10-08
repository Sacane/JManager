package fr.sacane.jmanager.infrastructure.spi.adapters

import jakarta.mail.Message
import jakarta.mail.Session
import jakarta.mail.internet.MimeMessage
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.springframework.mail.javamail.JavaMailSender
import java.time.LocalDateTime
import java.util.Properties

private const val APP_URL = "https://jmanager.test"

class PasswordResetEmailsTest {

    private val mailSender: JavaMailSender = mock {
        on { createMimeMessage() } doAnswer { MimeMessage(Session.getInstance(Properties())) }
    }
    private val adapter = SpringMailNotificationAdapter(mailSender, "noreply@jmanager.test", APP_URL)

    // The fragment never reaches a server: neither access logs nor a Referer header can leak it.
    @Test
    fun `shouldLinkToTheResetPageWithTheTokenInTheFragment`() {
        adapter.sendPasswordResetEmail("johan@example.com", "raw-token-123")

        val sent = sentMessage()
        assertThat(sent.getRecipients(Message.RecipientType.TO).single().toString()).isEqualTo("johan@example.com")
        assertThat(sent.subject).isNotBlank()
        assertThat(body(sent)).contains("$APP_URL/reset-password#token=raw-token-123")
        assertThat(body(sent)).doesNotContain("reset-password?token=")
    }

    @Test
    fun `shouldSayTheLinkLastsThirtyMinutesAndCanBeIgnored`() {
        adapter.sendPasswordResetEmail("johan@example.com", "raw-token-123")

        val text = body(sentMessage())
        assertThat(text).contains("30 minutes")
        assertThat(text).contains("ignorer cet e-mail")
    }

    // The change time is stored in UTC; the reader lives in France.
    @Test
    fun `shouldTellWhenThePasswordChangedInFrenchTime`() {
        adapter.sendPasswordChangedEmail("johan@example.com", LocalDateTime.of(2026, 10, 8, 9, 5))

        val sent = sentMessage()
        assertThat(sent.getRecipients(Message.RecipientType.TO).single().toString()).isEqualTo("johan@example.com")
        assertThat(body(sent)).contains("8 octobre 2026 à 11:05")
    }

    @Test
    fun `shouldTellWhatToDo_whenTheUserDidNotMakeTheChange`() {
        adapter.sendPasswordChangedEmail("johan@example.com", LocalDateTime.of(2026, 10, 8, 9, 5))

        val text = body(sentMessage())
        assertThat(text).contains("Si vous n'êtes pas à l'origine de ce changement")
        assertThat(text).contains("$APP_URL/forgot-password")
    }

    private fun sentMessage(): MimeMessage {
        val captor = argumentCaptor<MimeMessage>()
        verify(mailSender).send(captor.capture())
        return captor.firstValue
    }

    private fun body(message: MimeMessage): String = message.content as String
}
