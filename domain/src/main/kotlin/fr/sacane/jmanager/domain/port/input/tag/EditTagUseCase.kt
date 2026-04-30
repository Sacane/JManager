package fr.sacane.jmanager.domain.port.input.tag

import fr.sacane.jmanager.domain.hexadoc.DomainService
import fr.sacane.jmanager.domain.hexadoc.Port
import fr.sacane.jmanager.domain.hexadoc.Side
import fr.sacane.jmanager.domain.models.SessionToken
import fr.sacane.jmanager.domain.models.Tag
import fr.sacane.jmanager.domain.port.output.SessionManager
import fr.sacane.jmanager.domain.port.output.repository.TagRepository
import fr.sacane.jmanager.domain.port.input.Command
import fr.sacane.jmanager.domain.port.input.CommandHandler
import fr.sacane.jmanager.domain.utils.*

data class EditTagCommand(
    val token: SessionToken,
    val tag: Tag
) : Command<Tag>

@Port(Side.APPLICATION)
interface EditTagUseCase : CommandHandler<EditTagCommand, Tag> {
    override val commandClass get() = EditTagCommand::class
}

@DomainService
class EditTagService(
    private val tagRepository: TagRepository,
    private val session: SessionManager
) : EditTagUseCase {

    private fun <S> domainFailure(state: ResultState, detail: String, key: String): Result<S> {
        return failure(state, DomainError(state.code, key, detail))
    }

    override fun handle(command: EditTagCommand): Result<Tag> = session.authenticate(command.token) {
        val tagId = command.tag.id
        if (tagId == null || !tagRepository.existsById(tagId)) {
            return@authenticate domainFailure(
                ResultState.NOT_FOUND,
                "Tag with id ${command.tag.id} has not been found",
                "domain.tag.edit.not_found"
            )
        }
        if (tagRepository.existsAnotherTagByLabel(it, command.tag)) {
            return@authenticate domainFailure(
                ResultState.TAG_LABEL_ALREADY_TAKEN,
                "Label '${command.tag.label}' is already taken",
                "domain.tag.edit.label_already_taken"
            )
        }
        if (command.tag.isDefault) {
            return@authenticate domainFailure(
                ResultState.TAG_SHOULD_NOT_BE_DEFAULT,
                "Tag should not be default",
                "domain.tag.edit.default_forbidden"
            )
        }
        val save = tagRepository.patch(command.tag)
            ?: return@authenticate domainFailure(
                ResultState.NOT_FOUND,
                "User has not been found",
                "domain.tag.edit.user_not_found"
            )
        success(save)
    }
}
