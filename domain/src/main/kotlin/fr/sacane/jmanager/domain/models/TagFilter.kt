package fr.sacane.jmanager.domain.models

import java.util.UUID

/**
 * The tag the transactions of a period are filtered on: a tag keeps its sub-tags along, a sub-tag
 * stands alone. Without a tag, every transaction is kept.
 */
class TagFilter private constructor(private val tagId: UUID?) {

    fun matches(tag: Tag?): Boolean =
        tagId == null || tag?.id == tagId || (tag as? Tag.Personal)?.parentId == tagId

    companion object {
        fun of(tagId: UUID?): TagFilter = TagFilter(tagId)
    }
}
