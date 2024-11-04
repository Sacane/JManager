package fr.sacane.jmanager.domain.port.spi

import fr.sacane.jmanager.domain.models.SubscriptionComplete
import fr.sacane.jmanager.domain.models.UserId

interface SubscriptionRepository {
    fun addSubscription(userId: UserId, subscription: SubscriptionComplete): SubscriptionComplete?
}