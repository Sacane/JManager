package fr.sacane.jmanager.domain.models

import java.time.LocalDate

sealed interface SubscriptionFrom {
    val fromSubscription: Boolean
    val subscriptionId: Long?
}

class NotFromSubscription: SubscriptionFrom {
    override val fromSubscription: Boolean = false
    override val subscriptionId: Long? = null
}

class CreatedFromSubscription(override val subscriptionId: Long): SubscriptionFrom {
    override val fromSubscription: Boolean = false
}


data class Subscription(
    val amount: Amount,
    val label: String,
    val startDate: LocalDate,
    val isIncome: Boolean
)

data class SubscriptionComplete(
    val subscription: Subscription,
    val linkedAccountIds: List<Long>,
    val id: Long? = null,
)