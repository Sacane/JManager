package fr.sacane.jmanager.domain.port.api

import fr.sacane.jmanager.domain.models.Response
import fr.sacane.jmanager.domain.models.SubscriptionComplete
import fr.sacane.jmanager.domain.models.UserId

interface SubscriptionFeaturePrimaryPort {
    fun create(
        userId: UserId,
        subscription: SubscriptionComplete
    ): Response<Nothing>
}