package fr.sacane.jmanager.domain.port.input.tag

import fr.sacane.jmanager.domain.hexadoc.DomainService
import fr.sacane.jmanager.domain.port.spi.SessionManager
import fr.sacane.jmanager.domain.port.spi.repository.RegularTransactionRepository
import fr.sacane.jmanager.domain.port.spi.repository.TagRepository
import fr.sacane.jmanager.domain.port.spi.repository.TransactionRepository
import fr.sacane.jmanager.domain.utils.*

@DomainService
class DeleteTagService(
    private val tagRepository: TagRepository,
    private val transactionRepository: TransactionRepository,
    private val regularTransactionRepository: RegularTransactionRepository,
    private val session: SessionManager
) : DeleteTagUseCase {

    private fun <S> domainFailure(state: ResultState, detail: String, key: String): Result<S> {
        return failure(state, DomainError(state.code, key, detail))
    }

    override fun handle(command: DeleteTagCommand): Result<Nothing> = session.authenticate(command.token) {
        val isUsedInTransactions = transactionRepository.isPersonalTagUsed(command.tagId)
        val isUsedInRegular = regularTransactionRepository.isPersonalTagUsed(command.tagId)
        if ((isUsedInTransactions || isUsedInRegular) && !command.force) {
            return@authenticate domainFailure(
                ResultState.TAG_IN_USE,
                "Tag with id ${command.tagId} is used in existing transactions",
                "domain.tag.delete.tag_in_use"
            )
        }
        if (command.force && (isUsedInTransactions || isUsedInRegular)) {
            val defaultTag = tagRepository.defaultTag()
            if (isUsedInTransactions) transactionRepository.replacePersonalTagByDefault(command.tagId, defaultTag)
            if (isUsedInRegular) regularTransactionRepository.replacePersonalTagByDefault(command.tagId, defaultTag)
        }
        if (!tagRepository.deleteById(command.tagId)) {
            return@authenticate domainFailure(
                ResultState.NOT_FOUND,
                "Tag with id ${command.tagId} has not been found",
                "domain.tag.delete.not_found"
            )
        }
        success()
    }
}
