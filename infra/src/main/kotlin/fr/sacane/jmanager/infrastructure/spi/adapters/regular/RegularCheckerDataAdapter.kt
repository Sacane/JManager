package fr.sacane.jmanager.infrastructure.spi.adapters.regular

import fr.sacane.jmanager.domain.hexadoc.Adapter
import fr.sacane.jmanager.domain.hexadoc.Side
import fr.sacane.jmanager.domain.port.spi.RegularChecker
import fr.sacane.jmanager.infrastructure.spi.repositories.RecurringCheckTransactionJpaRepository
import org.springframework.stereotype.Component
import java.time.Month

@Adapter(Side.INFRASTRUCTURE)
@Component
class RegularCheckerDataAdapter(
    private val recurringCheckTransactionJpaRepository: RecurringCheckTransactionJpaRepository,
    private val regularTransactionOperatorAdapter: RegularTransactionOperatorAdapter
): RegularChecker {
    override fun check(bookletId: Long, year: Int, month: Month): Boolean {
        TODO("Not yet implemented")
    }

    override fun markAsVerified(bookletId: Long, year: Int, month: Month) {
        TODO("Not yet implemented")
    }

}