package fr.sacane.jmanager.domain.fake

import fr.sacane.jmanager.domain.models.SubscriptionPlan
import fr.sacane.jmanager.domain.port.output.NotificationPort

class FakeNotificationPort : NotificationPort {

    data class SentWelcomeEmail(
        val username: String,
        val email: String,
        val subscriptionPlan: SubscriptionPlan,
    )

    data class SentVerificationEmail(
        val email: String,
        val token: String,
    )

    val sentEmails: MutableList<SentWelcomeEmail> = mutableListOf()
    val sentVerificationEmails: MutableList<SentVerificationEmail> = mutableListOf()

    override fun sendWelcomeEmail(username: String, email: String, subscriptionPlan: SubscriptionPlan) {
        sentEmails.add(SentWelcomeEmail(username, email, subscriptionPlan))
    }

    override fun sendVerificationEmail(email: String, token: String) {
        sentVerificationEmails.add(SentVerificationEmail(email, token))
    }

    fun clear() {
        sentEmails.clear()
        sentVerificationEmails.clear()
    }
}
