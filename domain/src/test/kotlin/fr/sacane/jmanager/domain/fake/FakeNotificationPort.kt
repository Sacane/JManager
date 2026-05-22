package fr.sacane.jmanager.domain.fake

import fr.sacane.jmanager.domain.port.output.NotificationPort

class FakeNotificationPort : NotificationPort {
    val sentEmails: MutableList<Pair<String, String>> = mutableListOf()

    override fun sendWelcomeEmail(username: String, email: String) {
        sentEmails.add(username to email)
    }

    fun clear() {
        sentEmails.clear()
    }
}
