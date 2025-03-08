package fr.sacane.jmanager.infrastructure.spi.adapters

import fr.sacane.jmanager.domain.models.SubscriptionComplete
import fr.sacane.jmanager.domain.models.UserId
import fr.sacane.jmanager.domain.port.spi.SubscriptionRepository
import fr.sacane.jmanager.infrastructure.spi.repositories.SubscriptionJpaRepository
import fr.sacane.jmanager.infrastructure.spi.repositories.UserPostgresRepository
import jakarta.transaction.Transactional
import org.springframework.stereotype.Repository

@Repository
class SubscriptionJpaRepositoryAdapter(
    private val subscriptionJpaRepository: SubscriptionJpaRepository,
    private val userJpaRepository: UserPostgresRepository,
    private val subscriptionMapper: SubscriptionMapper
): SubscriptionRepository {
    @Transactional
    override fun addSubscription(userId: UserId, subscription: SubscriptionComplete): SubscriptionComplete? {
        val user = userJpaRepository.findByIdWithSubscription(userId.value!!) ?: return null
        val subscription1 = subscriptionMapper.asSubscriptionResource(subscription, user)
        user.addSubscription(subscription1)
        userJpaRepository.save(user)
        return subscriptionMapper.toDomain(subscriptionJpaRepository.save(subscription1))

    }

}