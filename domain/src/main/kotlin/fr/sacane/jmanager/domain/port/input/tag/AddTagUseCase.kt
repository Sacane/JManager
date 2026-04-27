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

data class AddTagCommand(
    val token: SessionToken,
    val tag: Tag
) : Command<Tag>

@Port(Side.APPLICATION)
interface AddTagUseCase : CommandHandler<AddTagCommand, Tag> {
    override val commandClass get() = AddTagCommand::class
}

@DomainService
class AddTagService(
    private val tagRepository: TagRepository,
    private val session: SessionManager
) : AddTagUseCase {

    private fun <S> domainFailure(state: ResultState, detail: String, key: String): Result<S> {
        return failure(state, DomainError(state.code, key, detail))
    }

    override fun handle(command: AddTagCommand): Result<Tag> = session.authenticate(command.token) {
        if (command.tag.isDefault || tagRepository.existsByLabelAndUserId(it, command.tag)) {
            return@authenticate domainFailure(
                ResultState.TAG_LABEL_ALREADY_TAKEN,
                "Label '${command.tag.label}' is already taken by the user ${it.value}",
                "domain.tag.add.label_already_taken"
            )
        }
        val save = tagRepository.save(it, command.tag)
            ?: return@authenticate domainFailure(
                ResultState.NOT_FOUND,
                "User has not been found",
                "domain.tag.add.user_not_found"
            )
        success(save)
    }
}
