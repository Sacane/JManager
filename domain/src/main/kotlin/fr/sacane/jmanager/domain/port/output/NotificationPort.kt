package fr.sacane.jmanager.domain.port.output

import fr.sacane.jmanager.domain.hexadoc.Port
import fr.sacane.jmanager.domain.hexadoc.Side
import fr.sacane.jmanager.domain.models.SubscriptionPlan

@Port(Side.INFRASTRUCTURE)
interface NotificationPort {

    /**
     * Sends a single welcome email that also contains the email-verification link.
     * Used by registration flows (self-registration and admin creation) so the user
     * receives exactly one email per sign-up.
     *
     * @param username the registered username
     * @param email the destination address
     * @param subscriptionPlan drives the welcome message variant (FREE, BETA_TESTER, PREMIUM)
     * @param verificationToken the raw token; the adapter builds the full verification URL
     */
    fun sendWelcomeWithVerificationEmail(
        username: String,
        email: String,
        subscriptionPlan: SubscriptionPlan,
        verificationToken: String,
    )

    /**
     * Sends a standalone verification email.
     * Used exclusively by the resend-verification flow when the user already exists.
     *
     * @param email the destination address
     * @param token the raw verification token to embed in the link
     */
    fun sendVerificationEmail(email: String, token: String)
}
