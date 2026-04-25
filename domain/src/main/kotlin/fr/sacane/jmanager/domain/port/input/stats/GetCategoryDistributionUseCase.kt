package fr.sacane.jmanager.domain.port.input.stats

import fr.sacane.jmanager.domain.hexadoc.Port
import fr.sacane.jmanager.domain.hexadoc.Side
import fr.sacane.jmanager.domain.models.CategoryDistributionOutput
import fr.sacane.jmanager.domain.models.SessionToken
import fr.sacane.jmanager.domain.utils.Result
import java.time.LocalDate
import java.util.UUID

@Port(Side.APPLICATION)
interface GetCategoryDistributionUseCase {
    fun getCategoryDistribution(
        token: SessionToken,
        bookletId: UUID?,
        startDate: LocalDate?,
        endDate: LocalDate?
    ): Result<CategoryDistributionOutput>
}
