package fr.sacane.jmanager.infrastructure.spi.repositories

import fr.sacane.jmanager.infrastructure.spi.entity.RegularRecurringCheckEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository

@Repository
interface RecurringCheckTransactionJpaRepository: JpaRepository<RegularRecurringCheckEntity, Long> {
    @Query("SELECT r FROM RegularRecurringCheckEntity r WHERE r.month = :month AND r.year = :year AND r.id = :userId")
    fun findByMonthYearAndUser(month: Int, year: Int, userId: Long): RegularRecurringCheckEntity?
}