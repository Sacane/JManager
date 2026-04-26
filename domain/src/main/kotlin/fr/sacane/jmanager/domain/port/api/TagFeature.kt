package fr.sacane.jmanager.domain.port.api

import fr.sacane.jmanager.domain.hexadoc.Port
import fr.sacane.jmanager.domain.hexadoc.Side
import fr.sacane.jmanager.domain.models.Tag
import fr.sacane.jmanager.domain.models.SessionToken
import fr.sacane.jmanager.domain.utils.*
import java.util.UUID

@Deprecated("Use individual use case interfaces from domain.port.input.tag instead")
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
    fun addTag(token: SessionToken, tag: Tag): Result<Tag>

    /**
     * Retrieve all tags visible to the authenticated user.
     *
     * This commonly includes the user's own tags and application-wide default tags.
     *
     * @param token Authentication token identifying the user session.
     * @return Result containing a list of Tag on success.
     */
    fun getAllTags(token: SessionToken): Result<List<Tag>>

    /**
     * Ensure the application's default tags are present in the repository.
     *
     * This operation is idempotent: if default tags already exist it does nothing.
     */
    fun addDefaultTags()

    /**
     * Delete a tag by its identifier.
     *
     * If the tag is currently assigned to one or more transactions or regular transactions and
     * [force] is `false`, the operation is rejected with [fr.sacane.jmanager.domain.utils.ResultState.TAG_IN_USE].
     * When [force] is `true`, all transactions (regular and normal) that reference the tag are
     * reassigned to the default tag before the deletion is performed.
     *
     * @param token Authentication token identifying the user session.
     * @param tagId Unique identifier of the tag to delete.
     * @param force When true, reassign affected transactions to the default tag and proceed with deletion.
     * @return Result with no value on success, or a failure when the tag was not found or is in use without force.
     */
    fun deleteTag(token: SessionToken, tagId: UUID, force: Boolean = false): Result<Nothing>

    /**
     * Retrieve the global default tag.
     *
     * @param token Authentication token identifying the user session.
     * @return Result containing the default Tag, or a notFound failure when no default tag exists.
     */
    fun defaultTag(token: SessionToken): Result<Tag>

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
    fun editTag(token: SessionToken, tag: Tag): Result<Tag>
}