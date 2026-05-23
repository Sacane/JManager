package fr.sacane.jmanager.domain.fake

import fr.sacane.jmanager.domain.models.SubscriptionPlan
import fr.sacane.jmanager.domain.port.output.NotificationPort

class FakeNotificationPort : NotificationPort {

    data class SentEmail(
        val username: String,
        val email: String,
        val subscriptionPlan: SubscriptionPlan,
    )

    val sentEmails: MutableList<SentEmail> = mutableListOf()

    override fun sendWelcomeEmail(username: String, email: String, subscriptionPlan: SubscriptionPlan) {
        sentEmails.add(SentEmail(username, email, subscriptionPlan))
    }

    fun clear() {
        sentEmails.clear()
    }
}
