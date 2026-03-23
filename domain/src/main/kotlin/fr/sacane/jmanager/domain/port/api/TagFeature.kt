package fr.sacane.jmanager.domain.port.api

import fr.sacane.jmanager.domain.hexadoc.DomainService
import fr.sacane.jmanager.domain.hexadoc.Port
import fr.sacane.jmanager.domain.hexadoc.Side
import fr.sacane.jmanager.domain.models.Tag
import fr.sacane.jmanager.domain.models.defaultTags
import fr.sacane.jmanager.domain.port.spi.SessionManager
import fr.sacane.jmanager.domain.port.spi.repository.TagRepository
import fr.sacane.jmanager.domain.utils.*
import java.util.UUID

@Port(Side.APPLICATION)
/**
 * Application port: TagFeature
 *
 * High-level API for tag-related use-cases exposed by the domain to the application side.
 * Implementations must perform authentication using the provided token and return a
 * domain Result<T> describing success or failure states.
 *
 * Contract notes:
 * - `token` parameters represent a session/authentication token.
 * - methods returning `Result<T>` wrap successful values or domain error states (failure/not found).
 */
sealed interface TagFeature {
    /**
     * Create a new tag for the authenticated user.
     *
     * @param token Authentication token identifying the user session.
     * @param tag Tag object to create. Should not be a default tag.
     * @return Result containing the saved Tag on success. Possible failure states include:
     *         - TAG_LABEL_ALREADY_TAKEN when the user already has a tag with the same label.
     *         - notFound when the user cannot be resolved.
     */
    fun addTag(token: String, tag: Tag): Result<Tag>

    /**
     * Retrieve all tags visible to the authenticated user.
     *
     * This commonly includes the user's own tags and application-wide default tags.
     *
     * @param token Authentication token identifying the user session.
     * @return Result containing a list of Tag on success.
     */
    fun getAllTags(token: String): Result<List<Tag>>

    /**
     * Ensure the application's default tags are present in the repository.
     *
     * This operation is idempotent: if default tags already exist it does nothing.
     */
    fun addDefaultTags()

    /**
     * Delete a tag by its identifier.
     *
     * @param token Authentication token identifying the user session.
     * @param tagId Unique identifier of the tag to delete.
     * @return Result with no value on success, or a notFound failure when the tag was not found.
     */
    fun deleteTag(token: String, tagId: UUID): Result<Nothing>

    /**
     * Retrieve the global default tag.
     *
     * @param token Authentication token identifying the user session.
     * @return Result containing the default Tag, or a notFound failure when no default tag exists.
     */
    fun defaultTag(token: String): Result<Tag>

    /**
     * Update an existing tag.
     *
     * Preconditions:
     * - The provided Tag must have a non-null id that exists in the repository.
     * - The tag must not be marked as default.
     * - No other tag with the same label must exist for the same user.
     *
     * @param token Authentication token identifying the user session.
     * @param tag Tag object containing updated fields (id must be provided).
     * @return Result containing the updated Tag on success, or appropriate failure states.
     */
    fun editTag(token: String, tag: Tag): Result<Tag>
}

@DomainService
class TagFeatureImpl(
    private val tagRepository: TagRepository,
    private val session: SessionManager
): TagFeature {
    private fun <S> domainFailure(state: ResultState, detail: String, key: String): Result<S> {
        return failure(state, DomainError(state.code, key, detail))
    }

    override fun addTag(token: String, tag: Tag): Result<Tag> = session.authenticate(token){
        if(tag.isDefault || tagRepository.existsByLabelAndUserId(it, tag)) {
            return@authenticate domainFailure(
                ResultState.TAG_LABEL_ALREADY_TAKEN,
                "Label '${tag.label}' is already taken by the user ${it.value}",
                "domain.tag.add.label_already_taken"
            )
        }
        val save = tagRepository.save(it, tag)
            ?: return@authenticate domainFailure(
                ResultState.NOT_FOUND,
                "User has not been found",
                "domain.tag.add.user_not_found"
            )
        success(save)
    }

    override fun getAllTags(token: String): Result<List<Tag>> = session.authenticate(token) {
        success(tagRepository.getAllDefault(it))
    }

    override fun addDefaultTags() {
        if(tagRepository.existsDefault()){
            return
        }
        tagRepository.saveAll(defaultTags)
    }

    override fun deleteTag(token: String, tagId: UUID): Result<Nothing> = session.authenticate(token){
        if(!tagRepository.deleteById(tagId)) {
            return@authenticate domainFailure(
                ResultState.NOT_FOUND,
                "Tag with id $tagId has not been found",
                "domain.tag.delete.not_found"
            )
        }
        success()
    }

    override fun defaultTag(token: String): Result<Tag> = session.authenticate(token) {
        val tagResult = tagRepository.defaultTag()
        return@authenticate success(tagResult)
    }

    override fun editTag(token: String, tag: Tag): Result<Tag> = session.authenticate(token) {
        if(tag.id == null || !tagRepository.existsById(tag.id)) {
            return@authenticate domainFailure(
                ResultState.NOT_FOUND,
                "Tag with id ${tag.id} has not been found",
                "domain.tag.edit.not_found"
            )
        }
        if(
            tagRepository.existsAnotherTagByLabel(it, tag)
        ) {
            return@authenticate domainFailure(
                ResultState.TAG_LABEL_ALREADY_TAKEN,
                "Label '${tag.label}' is already taken",
                "domain.tag.edit.label_already_taken"
            )
        }
        if(tag.isDefault) {
            return@authenticate domainFailure(
                ResultState.TAG_SHOULD_NOT_BE_DEFAULT,
                "Tag should not be default",
                "domain.tag.edit.default_forbidden"
            )
        }
        val save = tagRepository.patch(tag)
            ?: return@authenticate domainFailure(
                ResultState.NOT_FOUND,
                "User has not been found",
                "domain.tag.edit.user_not_found"
            )
        success(save)
    }
}