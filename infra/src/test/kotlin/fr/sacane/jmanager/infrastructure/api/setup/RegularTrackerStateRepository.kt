package fr.sacane.jmanager.infrastructure.api.setup

import fr.sacane.jmanager.domain.models.transaction.regular.RegularTransactionTracker
import fr.sacane.jmanager.infrastructure.State
import fr.sacane.jmanager.infrastructure.spi.entity.transaction.JpaRegularTransactionTrackerRepository
import fr.sacane.jmanager.infrastructure.spi.entity.transaction.RegularTransactionTrackerEntity
import org.springframework.stereotype.Component

@Component
class RegularTrackerStateRepository(
    private val trackerRepository: JpaRegularTransactionTrackerRepository
): State<RegularTransactionTracker, RegularTransactionTracker> {
    override fun get(): Collection<RegularTransactionTracker> {
        return trackerRepository.findAll().map { it.toDomain() }
    }

    override fun init(initialState: Collection<RegularTransactionTracker>) {
        initialState.forEach {
            trackerRepository.save(RegularTransactionTrackerEntity.fromDomain(it))
        }
    }

    override fun clear() {
        trackerRepository.deleteAll()
    }

}