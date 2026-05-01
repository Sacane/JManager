package fr.sacane.jmanager.domain.port.input.regularTransaction

import fr.sacane.jmanager.domain.Paginator
import fr.sacane.jmanager.domain.hexadoc.DomainService
import fr.sacane.jmanager.domain.hexadoc.Port
import fr.sacane.jmanager.domain.hexadoc.Side
import fr.sacane.jmanager.domain.models.Page
import fr.sacane.jmanager.domain.models.UserId
import fr.sacane.jmanager.domain.models.transaction.regular.RegularTransaction
import fr.sacane.jmanager.domain.port.output.repository.RegularTransactionRepository
import fr.sacane.jmanager.domain.port.input.Query
import fr.sacane.jmanager.domain.port.input.QueryHandler
import fr.sacane.jmanager.domain.utils.Result
import fr.sacane.jmanager.domain.utils.success

data class GetAllRegularTransactionsQuery(
    val userId: UserId,
    val pageNumber: Int = 0,
    val pageSize: Int = 10
) : Query<Page<RegularTransaction>>

@Port(Side.APPLICATION)
interface GetAllRegularTransactionsUseCase : QueryHandler<GetAllRegularTransactionsQuery, Page<RegularTransaction>> {
    override val queryClass get() = GetAllRegularTransactionsQuery::class
}

@DomainService
class GetAllRegularTransactionsService(
    private val regularTransactionRepository: RegularTransactionRepository,
    private val paginator: Paginator
) : GetAllRegularTransactionsUseCase {

    override fun handle(query: GetAllRegularTransactionsQuery): Result<Page<RegularTransaction>> {
        val userId = query.userId
        val page = paginator.paginate(query.pageNumber, query.pageSize) {
            regularTransactionRepository.getAllRegularTransactions(userId)
        }
        return success(page)
    }
}
