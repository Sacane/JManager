package fr.sacane.jmanager.domain.port.input.tag

import fr.sacane.jmanager.domain.hexadoc.DomainService
import fr.sacane.jmanager.domain.hexadoc.Port
import fr.sacane.jmanager.domain.hexadoc.Side
import fr.sacane.jmanager.domain.models.UserId
import fr.sacane.jmanager.domain.port.output.repository.RegularTransactionRepository
import fr.sacane.jmanager.domain.port.output.repository.TagRepository
import fr.sacane.jmanager.domain.port.output.repository.TransactionRepository
import fr.sacane.jmanager.domain.port.input.Command
import fr.sacane.jmanager.domain.port.input.CommandHandler
import fr.sacane.jmanager.domain.utils.*
import java.util.UUID

data class DeleteTagCommand(
    val userId: UserId,
    val tagId: UUID,
    val force: Boolean = false
) : Command<Nothing>

@Port(Side.APPLICATION)
interface DeleteTagUseCase : CommandHandler<DeleteTagCommand, Nothing> {
    override val commandClass get() = DeleteTagCommand::class
}

@DomainService
class DeleteTagService(
    private val tagRepository: TagRepository,
    private val transactionRepository: TransactionRepository,
    private val regularTransactionRepository: RegularTransactionRepository
) : DeleteTagUseCase {

    private fun <S> domainFailure(state: ResultState, detail: String, key: String): Result<S> {
        return failure(state, DomainError(state.code, key, detail))
    }

    override fun handle(command: DeleteTagCommand): Result<Nothing> {
        val subTags = tagRepository.findSubTagsByParentId(command.tagId)
        val allTagIds = listOf(command.tagId) + subTags.mapNotNull { it.id }

        val isUsedInTransactions = allTagIds.any { transactionRepository.isPersonalTagUsed(it) }
        val isUsedInRegular = allTagIds.any { regularTransactionRepository.isPersonalTagUsed(it) }

        if ((isUsedInTransactions || isUsedInRegular) && !command.force) {
            return domainFailure(
                ResultState.TAG_IN_USE,
                "Tag with id ${command.tagId} is used in existing transactions",
                "domain.tag.delete.tag_in_use"
            )
        }
        if (command.force && (isUsedInTransactions || isUsedInRegular)) {
            val defaultTag = tagRepository.defaultTag()
            for (tagId in allTagIds) {
                if (transactionRepository.isPersonalTagUsed(tagId)) {
                    transactionRepository.replacePersonalTagByDefault(tagId, defaultTag)
                }
                if (regularTransactionRepository.isPersonalTagUsed(tagId)) {
                    regularTransactionRepository.replacePersonalTagByDefault(tagId, defaultTag)
                }
            }
        }

        for (subTag in subTags) {
            subTag.id?.let { tagRepository.deleteById(it) }
        }

        if (!tagRepository.deleteById(command.tagId)) {
            return domainFailure(
                ResultState.NOT_FOUND,
                "Tag with id ${command.tagId} has not been found",
                "domain.tag.delete.not_found"
            )
        }
        return success()
    }
}
