package fr.sacane.jmanager.domain.port.input.tag

import fr.sacane.jmanager.domain.hexadoc.DomainService
import fr.sacane.jmanager.domain.hexadoc.Port
import fr.sacane.jmanager.domain.hexadoc.Side
import fr.sacane.jmanager.domain.models.Tag
import fr.sacane.jmanager.domain.models.UserId
import fr.sacane.jmanager.domain.port.output.repository.TagRepository
import fr.sacane.jmanager.domain.port.input.Command
import fr.sacane.jmanager.domain.port.input.CommandHandler
import fr.sacane.jmanager.domain.utils.*

data class AddTagCommand(
    val userId: UserId,
    val tag: Tag
) : Command<Tag>

@Port(Side.APPLICATION)
interface AddTagUseCase : CommandHandler<AddTagCommand, Tag> {
    override val commandClass get() = AddTagCommand::class
}

@DomainService
class AddTagService(
    private val tagRepository: TagRepository
) : AddTagUseCase {

    private fun <S> domainFailure(state: ResultState, detail: String, key: String): Result<S> {
        return failure(state, DomainError(state.code, key, detail))
    }

    override fun handle(command: AddTagCommand): Result<Tag> {
        val userId = command.userId
        if (command.tag.isDefault || tagRepository.existsByLabelAndUserId(userId, command.tag)) {
            return domainFailure(
                ResultState.TAG_LABEL_ALREADY_TAKEN,
                "Label '${command.tag.label}' is already taken by the user ${userId.value}",
                "domain.tag.add.label_already_taken"
            )
        }
        val save = tagRepository.save(userId, command.tag)
            ?: return domainFailure(
                ResultState.NOT_FOUND,
                "User has not been found",
                "domain.tag.add.user_not_found"
            )
        return success(save)
    }
}
