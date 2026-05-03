package fr.sacane.jmanager.domain.port.input.tag

import fr.sacane.jmanager.domain.hexadoc.DomainService
import fr.sacane.jmanager.domain.hexadoc.Port
import fr.sacane.jmanager.domain.hexadoc.Side
import fr.sacane.jmanager.domain.models.Tag
import fr.sacane.jmanager.domain.models.UserId
import fr.sacane.jmanager.domain.port.input.Command
import fr.sacane.jmanager.domain.port.input.CommandHandler
import fr.sacane.jmanager.domain.port.output.repository.TagRepository
import fr.sacane.jmanager.domain.utils.*
import java.util.UUID

data class CreateSubTagCommand(
    val userId: UserId,
    val tag: Tag.Personal,
    val parentId: UUID
) : Command<Tag>

@Port(Side.APPLICATION)
interface CreateSubTagUseCase : CommandHandler<CreateSubTagCommand, Tag> {
    override val commandClass get() = CreateSubTagCommand::class
}

@DomainService
class CreateSubTagService(
    private val tagRepository: TagRepository
) : CreateSubTagUseCase {

    private fun <S> domainFailure(state: ResultState, detail: String, key: String): Result<S> {
        return failure(state, DomainError(state.code, key, detail))
    }

    override fun handle(command: CreateSubTagCommand): Result<Tag> {
        val parentTag = tagRepository.findById(command.parentId)
            ?: return domainFailure(
                ResultState.NOT_FOUND,
                "Parent tag with id ${command.parentId} not found",
                "domain.tag.create_sub_tag.parent_not_found"
            )

        if (parentTag.isDefault) {
            return domainFailure(
                ResultState.INVALID,
                "Cannot create a sub-tag under a default tag",
                "domain.tag.create_sub_tag.parent_is_default"
            )
        }

        if (parentTag is Tag.Personal && parentTag.parentId != null) {
            return domainFailure(
                ResultState.TAG_PARENT_IS_SUBTAG,
                "Cannot create a sub-tag under another sub-tag (max depth = 2)",
                "domain.tag.create_sub_tag.parent_is_subtag"
            )
        }

        if (tagRepository.existsByLabelAndUserId(command.userId, command.tag)) {
            return domainFailure(
                ResultState.TAG_LABEL_ALREADY_TAKEN,
                "Label '${command.tag.label}' is already taken",
                "domain.tag.create_sub_tag.label_already_taken"
            )
        }

        val tagToSave = command.tag.copy(parentId = command.parentId)
        val saved = tagRepository.save(command.userId, tagToSave)
            ?: return domainFailure(
                ResultState.NOT_FOUND,
                "User not found",
                "domain.tag.create_sub_tag.user_not_found"
            )

        return success(saved)
    }
}
