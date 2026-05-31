package fr.sacane.jmanager.domain.fake

import fr.sacane.jmanager.domain.models.SubscriptionPlan
import fr.sacane.jmanager.domain.port.output.NotificationPort

class FakeNotificationPort : NotificationPort {

    data class SentCombinedEmail(
        val username: String,
        val email: String,
        val subscriptionPlan: SubscriptionPlan,
        val verificationToken: String,
    )

    data class SentVerificationEmail(
        val email: String,
        val token: String,
    )

    val sentCombinedEmails: MutableList<SentCombinedEmail> = mutableListOf()
    val sentVerificationEmails: MutableList<SentVerificationEmail> = mutableListOf()

    override fun sendWelcomeWithVerificationEmail(
        username: String,
        email: String,
        subscriptionPlan: SubscriptionPlan,
        verificationToken: String,
    ) {
        sentCombinedEmails.add(SentCombinedEmail(username, email, subscriptionPlan, verificationToken))
    }

    override fun sendVerificationEmail(email: String, token: String) {
        sentVerificationEmails.add(SentVerificationEmail(email, token))
    }

    fun clear() {
        sentCombinedEmails.clear()
        sentVerificationEmails.clear()
    }
}
