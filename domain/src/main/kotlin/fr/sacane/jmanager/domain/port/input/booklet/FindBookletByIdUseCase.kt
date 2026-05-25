package fr.sacane.jmanager.domain.port.input.booklet

import fr.sacane.jmanager.domain.hexadoc.DomainService
import fr.sacane.jmanager.domain.hexadoc.Port
import fr.sacane.jmanager.domain.hexadoc.Side
import fr.sacane.jmanager.domain.models.Booklet
import fr.sacane.jmanager.domain.models.UserId
import fr.sacane.jmanager.domain.port.output.repository.BookletRepository
import fr.sacane.jmanager.domain.port.input.MdcContextProvider
import fr.sacane.jmanager.domain.port.input.MdcKeys
import fr.sacane.jmanager.domain.port.input.Query
import fr.sacane.jmanager.domain.port.input.QueryHandler
import fr.sacane.jmanager.domain.utils.Result
import fr.sacane.jmanager.domain.utils.ResultState
import fr.sacane.jmanager.domain.utils.success
import java.util.UUID

data class FindBookletByIdQuery(
    val bookletId: UUID,
    val userId: UserId
) : Query<Booklet>, MdcContextProvider {
    override fun mdcContext() = mapOf(MdcKeys.BOOKLET_ID to bookletId.toString())
}

@Port(Side.APPLICATION)
interface FindBookletByIdUseCase : QueryHandler<FindBookletByIdQuery, Booklet> {
    override val queryClass get() = FindBookletByIdQuery::class
}

@DomainService
class FindBookletByIdService(
    private val bookletRepository: BookletRepository
) : FindBookletByIdUseCase {
    override fun handle(query: FindBookletByIdQuery): Result<Booklet> =
        bookletRepository.findBookletByIdWithTransactions(query.bookletId)?.run {
            success(this)
        } ?: bookletDomainFailure(
            ResultState.BOOKLET_NOT_FOUND,
            "Le compte est introuvable",
            "domain.booklet.find_by_id.not_found"
        )
}
