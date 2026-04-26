package fr.sacane.jmanager.domain.port.input.booklet

import fr.sacane.jmanager.domain.hexadoc.DomainService
import fr.sacane.jmanager.domain.models.Booklet
import fr.sacane.jmanager.domain.port.spi.SessionManager
import fr.sacane.jmanager.domain.port.spi.repository.BookletRepository
import fr.sacane.jmanager.domain.utils.Result
import fr.sacane.jmanager.domain.utils.ResultState
import fr.sacane.jmanager.domain.utils.success

@DomainService
class FindBookletByIdService(
    private val session: SessionManager,
    private val bookletRepository: BookletRepository
) : FindBookletByIdUseCase {
    override fun handle(query: FindBookletByIdQuery): Result<Booklet> = session.authenticate(query.token) {
        bookletRepository.findBookletByIdWithTransactions(query.bookletId)?.run {
            success(this)
        } ?: bookletDomainFailure(
            ResultState.BOOKLET_NOT_FOUND,
            "Le compte est introuvable",
            "domain.booklet.find_by_id.not_found"
        )
    }
}
