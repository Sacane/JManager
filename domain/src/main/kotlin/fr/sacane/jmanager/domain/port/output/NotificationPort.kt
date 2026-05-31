package fr.sacane.jmanager.domain.port.output

import fr.sacane.jmanager.domain.hexadoc.Port
import fr.sacane.jmanager.domain.hexadoc.Side
import fr.sacane.jmanager.domain.models.SubscriptionPlan

@Port(Side.INFRASTRUCTURE)
interface NotificationPort {
    /**
     * Sends a welcome email to a newly registered user.
     * The content adapts to the user's subscription plan.
     *
     * @param username the registered username
     * @param email the destination email address
     * @param subscriptionPlan the plan the user was registered under
     */
    fun sendWelcomeEmail(username: String, email: String, subscriptionPlan: SubscriptionPlan)

    /**
     * Sends a verification email carrying a raw token.
     * The infrastructure adapter is responsible for constructing the full verification link.
     *
     * @param email the destination address
     * @param token the raw verification token to embed in the link
     */
    fun sendVerificationEmail(email: String, token: String)
}
