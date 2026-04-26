package fr.sacane.jmanager.domain.port.input.regularTransaction

import fr.sacane.jmanager.domain.Paginator
import fr.sacane.jmanager.domain.hexadoc.DomainService
import fr.sacane.jmanager.domain.hexadoc.Port
import fr.sacane.jmanager.domain.hexadoc.Side
import fr.sacane.jmanager.domain.models.Page
import fr.sacane.jmanager.domain.models.SessionToken
import fr.sacane.jmanager.domain.models.transaction.regular.RegularTransaction
import fr.sacane.jmanager.domain.port.spi.SessionManager
import fr.sacane.jmanager.domain.port.spi.repository.RegularTransactionRepository
import fr.sacane.jmanager.domain.utils.Result
import fr.sacane.jmanager.domain.utils.success

data class GetAllRegularTransactionsQuery(
    val token: SessionToken,
    val pageNumber: Int = 0,
    val pageSize: Int = 10
)

@Port(Side.APPLICATION)
interface GetAllRegularTransactionsUseCase {
    fun handle(query: GetAllRegularTransactionsQuery): Result<Page<RegularTransaction>>
}

@DomainService
class GetAllRegularTransactionsService(
    private val regularTransactionRepository: RegularTransactionRepository,
    private val session: SessionManager,
    private val paginator: Paginator
) : GetAllRegularTransactionsUseCase {

    override fun handle(query: GetAllRegularTransactionsQuery): Result<Page<RegularTransaction>> {
        return session.authenticate(query.token) {
            val page = paginator.paginate(query.pageNumber, query.pageSize) {
                regularTransactionRepository.getAllRegularTransactions(it)
            }
            return@authenticate success(page)
        }
    }
}
