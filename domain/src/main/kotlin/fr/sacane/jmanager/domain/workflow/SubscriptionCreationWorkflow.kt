package fr.sacane.jmanager.domain.workflow

import fr.sacane.jmanager.domain.hexadoc.DomainService
import fr.sacane.jmanager.domain.models.Response
import fr.sacane.jmanager.domain.models.Response.Companion.ok
import fr.sacane.jmanager.domain.models.SubscriptionComplete
import fr.sacane.jmanager.domain.models.UserId
import fr.sacane.jmanager.domain.port.spi.UserRepository
import java.util.logging.Logger

@DomainService
class SubscriptionCreationWorkflow (
    private val userRepository : UserRepository,
) {
    companion object {
        private val LOG = Logger.getLogger(this::class.java.name)
    }
    fun create(
        userId: UserId,
        subscription: SubscriptionComplete
    ): Response<Nothing> {
        val user = userRepository.findUserByIdWithAccounts(userId) ?: return Response.notFound()
        val setAccountId = user.accounts.map { it.id }.toSet()
        if(subscription.linkedAccountIds.isNotEmpty() && !setAccountId.containsAll(subscription.linkedAccountIds)){
            val errorMsg = "Subscription's linked booklet's id must all be part of the user's booklet"
            LOG.severe(errorMsg)
            return Response.notFound(errorMsg)
        }
        user.addSubscription(subscription)
        return ok()
    }
}