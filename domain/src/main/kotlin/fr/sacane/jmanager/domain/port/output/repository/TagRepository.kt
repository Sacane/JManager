package fr.sacane.jmanager.domain.port.output.repository

import fr.sacane.jmanager.domain.models.Tag
import fr.sacane.jmanager.domain.models.UserId
import java.util.UUID

/**
 * SPI contract for persistence and retrieval of Tag aggregates.
 *
 * Implementations provide storage operations for user tags and default tags.
 * The domain relies on this abstraction to manage tags without knowing details of the storage.
 */
interface TagRepository {
    /**
     * Save a tag for the given user.
     *
     * @param userId Domain user identifier
     * @param tag Tag aggregate to persist
     * @return Persisted Tag or null on failure
     */
    fun save(userId: UserId, tag: Tag): Tag?

    /**
     * Retrieve all tags owned by the given user.
     *
     * @param userId Domain user identifier
     * @return List of Tag objects (may be empty)
     */
    fun getAll(userId: UserId): List<Tag>

    /**
     * Delete tags by label for the entire system or user depending on implementation semantics.
     *
     * @param label Label of the tag to delete
     */
    fun deleteByLabel(label: String)

    /**
     * Retrieve all tags visible to the user, including default/global tags.
     *
     * @param userId Domain user identifier
     * @return List of Tag objects (including defaults)
     */
    fun getAllDefault(userId: UserId): List<Tag>

    /**
     * Check whether a tag with the same label already exists for the user.
     *
     * @param userId Domain user identifier
     * @param tag Tag to check for existence
     * @return true if a tag with the same label exists for the user
     */
    fun existsByLabelAndUserId(userId: UserId, tag: Tag): Boolean

    /**
     * Save multiple default tags at once (bootstrap operation).
     *
     * @param defaultTags List of Tag objects to persist as defaults
     */
    fun saveAll(defaultTags: List<Tag>)

    /**
     * Returns whether the repository already contains default tags.
     *
     * @return true if default tags exist
     */
    fun existsDefault(): Boolean

    /**
     * Delete a tag by its unique identifier.
     *
     * @param tagId UUID of the tag to delete
     * @return true when deletion succeeded, false otherwise
     */
    fun deleteById(tagId: UUID): Boolean

    /**
     * Retrieve the global/default tag used when no tag has been set on a transaction.
     * Implementations may return a system-level default Tag.
     *
     * @return default Tag
     */
    fun defaultTag(): Tag

    /**
     * Patch (update) a tag entity.
     *
     * @param tag Tag object with updated fields (id must be present)
     * @return Updated Tag or null on failure
     */
    fun patch(tag: Tag): Tag?

    /**
     * Check whether another tag (different id) already exists with the same label for the user.
     *
     * @param userId Domain user identifier
     * @param tag Tag object to check (its id will be excluded in the search)
     * @return true if another tag exists with the same label
     */
    fun existsAnotherTagByLabel(userId: UserId, tag: Tag): Boolean

    /**
     * Check whether a tag exists by its identifier.
     *
     * @param tagId UUID of the tag
     * @return true if the tag exists
     */
    fun existsById(tagId: UUID): Boolean

    /**
     * Retrieve all sub-tags that have the given tag as their parent.
     *
     * @param parentId UUID of the parent tag
     * @return List of personal sub-tags (may be empty)
     */
    fun findSubTagsByParentId(parentId: UUID): List<Tag.Personal>

    /**
     * Check whether a tag has at least one sub-tag.
     *
     * @param tagId UUID of the tag to check
     * @return true if at least one sub-tag references this tag as parent
     */
    fun hasSubTags(tagId: UUID): Boolean

    /**
     * Find a tag by its identifier.
     *
     * @param tagId UUID of the tag
     * @return the Tag if found, null otherwise
     */
    fun findById(tagId: UUID): Tag?
}
